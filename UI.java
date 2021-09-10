import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Scanner;

public class UI {
    static private final String JDBC_URL = "jdbc:h2:~/csc365_lab7";
    static private final String JDBC_USER = "";
    static private final String JDBC_PASSWORD = "";
    static String[] cmds;
    static String temp;
    static Scanner scn = new Scanner(System.in);

    public static void printMainMenu(){
        System.out.println("Please select from the following options:");
        System.out.println("1 - Room and Rates");
        System.out.println("2 - Reservations");
        System.out.println("3 - Reservation Change");
        System.out.println("4 - Reservation Cancellation");
        System.out.println("5 - Revenue Summary");
        System.out.println("0 - Exit Program");
    }

    public static void getUserInput(){
        printMainMenu();
        temp = scn.nextLine();

        cmds = temp.split("\\s");
    }

    /*  output a list of rooms to the user sorted by name.
        Include all information from the lab7 rooms table, as well as
            -Next available check-in date
                (or “Today” if the room is currently unoccupied)
            -Date on which the next reservation in the room begins
                (or “None” if there are no future reservations)
     */
    private static void roomAndRates() throws SQLException {
        /*String roomCode = "";
        String roomName = "";
        int beds = 0;
        String bedType = "";
        int maxOcc = 0;
        float basePrice = 0;
        String decor = "";
        String nextAvailableCheckIn = "";
        String nextReservationDate = "";*/
        long millis=System.currentTimeMillis();
        java.sql.Date date=new java.sql.Date(millis);

        java.sql.Date checkInDate;
        java.sql.Date nextRes;
        String checkInFinal = "Today";
        String nextResFinal = "None";

        StringBuilder sql = new StringBuilder("with ");
        sql.append("rmReservations as (");
        sql.append("  select room, min(CheckIn) as nextReservationDate");
        sql.append("  from lab7_reservations as r1");
        sql.append("  where curdate() <= Checkout");
        sql.append("  group by r1.room),");

        sql.append("rmAvailability as (");
        sql.append("  select r.room, greatest(curdate(), max(r.checkout)) as nextAvailableCheckIn");
        sql.append("  from lab7_reservations as r");
        sql.append("  group by r.room)");

        sql.append("select RoomCode, RoomName, Beds, BedType, MaxOcc, BasePrice, Decor, " +
                "nextAvailableCheckIn, nextReservationDate");
        sql.append("  from lab7_rooms as rm");
        sql.append("  left outer join rmAvailability as ra");
        sql.append("    on rm.RoomCode = ra.room");
        sql.append("  left outer join rmReservations as rr");
        sql.append("    on rm.RoomCode = rr.room");
        sql.append("  order by rm.RoomCode");


        try (Connection conn = DriverManager.getConnection(JDBC_URL,
                JDBC_USER,
                JDBC_PASSWORD)) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql.toString())) {
                while (rs.next()) {
                    String RoomCode = rs.getString("RoomCode");
                    String RoomName = rs.getString("RoomName");
                    int Beds = rs.getInt("Beds");
                    String bedType = rs.getString("bedType");
                    int maxOcc = rs.getInt("maxOcc");
                    float basePrice = rs.getFloat("basePrice");
                    String decor = rs.getString("decor");
                    if(((checkInDate = rs.getDate("nextAvailableCheckIn")) != null) &&
                        (checkInDate.toString()).equals(date))
                         checkInFinal =checkInDate.toString();
                    if((nextRes = rs.getDate("nextReservationDate")) != null)
                        nextResFinal = nextRes.toString();
                    System.out.format("%s %s %d %s %d ($%.2f) %s %s %s %n",
                            RoomCode, RoomName, Beds, bedType, maxOcc, basePrice,decor, checkInFinal, nextResFinal);
                }
                System.out.println();
            }
        }
    }

    public static void reservations() throws SQLException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // cmds array now holds each selection, see for indices
        System.out.println("Please enter the following information separated by spaces:");
        System.out.println("First name");  // cmds[0]
        System.out.println("Last name"); // cmds[1]
        System.out.println("A room code to indicate the specific room desired"); // cmds[2]
        System.out.println("Beginning of stay [YYYY-MM-DD]:"); // cmds[3]
        System.out.println("End of stay [YYYY-MM-DD]:"); // cmds[4]
        System.out.println("Number of children"); // cmds[5]
        System.out.println("Number of Adults"); // cmds[6]

        temp = scn.nextLine();
        cmds = temp.split("\\s");

        // Delete this later, just to see whats goin on
        System.out.print(temp + "\n");

        int reservationCode = 0;

        double rate = 0.0;
        String bedType = "";

        int occupancy = 0;
        String roomName = "";

        // See if all dates in reservation block are available...
        try (Connection con = DriverManager.getConnection(JDBC_URL,
                JDBC_USER,
                JDBC_PASSWORD)) {
            try {
                PreparedStatement prepSQL = con.prepareStatement("select Room from lab7_reservations WHERE Room = ? and CheckIn >= ? and CheckOut <= ?;");
                prepSQL.setString(1, cmds[2]);
                java.sql.Date date3 = java.sql.Date.valueOf(cmds[3]);
                java.sql.Date date4 = java.sql.Date.valueOf(cmds[4]);
                prepSQL.setDate(2, date3);
                prepSQL.setDate(3, date4);
                ResultSet res = prepSQL.executeQuery();

                try (Statement stm2 = con.createStatement()) {
                    ResultSet res2 = stm2.executeQuery("SELECT max(CODE) as CODE from lab7_reservations;");

                    while (res2.next()) {
                        String code = res2.getString("CODE");
                        reservationCode = Integer.parseInt(code) + 1;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                LocalDate reservationCheckInDate = LocalDate.parse(cmds[3]);
                LocalDate reservationCheckOutDate = LocalDate.parse(cmds[4]);
                LocalDate currentDate = LocalDate.now();

                int c1 = currentDate.compareTo(reservationCheckInDate);
                int c2 = currentDate.compareTo(reservationCheckOutDate);

                if ((c1 <= 0 && c2 <= 0) && !res.next()) {
                    try (PreparedStatement ifSQL = con.prepareStatement("SELECT RoomName, maxOcc, basePrice, bedType from lab7_rooms where lab7_rooms.RoomCode = ?;")) {
                        // We want to get room information.. use room code given
                        ifSQL.setString(1, cmds[2]);

                        // Rate, bed type
                        ResultSet res3 = ifSQL.executeQuery();

                        while (res3.next()) {
                            rate = Float.parseFloat(res3.getString("basePrice"));
                            bedType = res3.getString("bedType");
                            occupancy = Integer.parseInt(res3.getString("maxOcc"));
                            roomName = res3.getString("RoomName");
                        }

                        // threshold
                        int totalOccupancy = Integer.parseInt(cmds[5]) + Integer.parseInt(cmds[6]);

                        if (totalOccupancy > occupancy) {
                            System.out.println("\nWe were unable to make your reservation due to a room occupancy conflict.");
                            System.out.println("Please try again.\n");
                        } else {
                            try (PreparedStatement prints = con.prepareStatement("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                                prints.setInt(1, reservationCode);
                                prints.setString(2, cmds[2]);
                                prints.setDate(3, (java.sql.Date) date3);
                                prints.setDate(4, (java.sql.Date) date4);
                                prints.setDouble(5, rate);
                                prints.setString(6, cmds[2]);
                                prints.setString(7, cmds[1]);
                                prints.setInt(8, Integer.parseInt(cmds[6]));
                                prints.setInt(9, Integer.parseInt(cmds[5]));

                                prints.executeUpdate();
                                con.commit();
                                //Update user on success of reservation.
                                System.out.println("*** Success ***");
                                System.out.println("First Name: " + cmds[0]);
                                System.out.println("LastName: " + cmds[1]);
                                System.out.println("RoomCode: " + cmds[2]);
                                System.out.println("RoomName: " + roomName);
                                System.out.println("BedType: " + bedType);
                                System.out.println("StartDate: " + date3);
                                System.out.println("EndDate: " + date4);
                                System.out.println("Number of Kids: " + cmds[5]);
                                System.out.println("Number of Adults: " + cmds[6]);
                                System.out.println("RESERVATION CODE: " + reservationCode);

                                // ----------- //

                                // total cost of stay ..
                                double totalCost = 0;

                                LocalDate checkIn = LocalDate.parse(cmds[3]);
                                LocalDate checkOut = LocalDate.parse(cmds[4]);
                                ZoneId timezone = ZoneId.of("America/Los_Angeles");

                                for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
                                    Calendar cal = Calendar.getInstance();
                                    ZonedDateTime zoned_time = date.atStartOfDay(timezone);

                                    Instant i = zoned_time.toInstant();
                                    java.util.Date d = java.util.Date.from(i);
                                    cal.setTime(d);

                                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                                    // weekend!! $ (1.10 x)
                                    if (dayOfWeek == 0 || dayOfWeek == 7)
                                        totalCost += (rate * 1.10);

                                        // weekday
                                    else if (dayOfWeek >= 2 && dayOfWeek <= 6)
                                        totalCost += (rate);
                                }
                                System.out.println("Total cost of stay: " + totalCost + "\n");

                            } catch (SQLException e) {
                                e.printStackTrace();
                                con.rollback();
                            }
                        }
                    } catch (SQLException e) {
                        con.rollback();
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("\nWe are unable to make your reservation. Reason: time conflict.");
                    System.out.println("Please try again.\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void reservationsChange() throws SQLException {
        System.out.println("Please enter the a reservation code then the" +
                " following reservation information to be changed," +
                " seperrated by spaces, or type noChange to leave as-is:");
        System.out.println("First name"); // cmds[1]
        System.out.println("Last name"); // cmds[2]
        System.out.println("Begin of stay"); // cmds[3]
        System.out.println("End of stay"); // cmds[4]
        System.out.println("Number of children"); // cmds[5]
        System.out.println("Number of Adults"); // cmds[6]

        temp = scn.nextLine();
        cmds = temp.split("\\s");

        // cmds array now holds each selection, see above for indices
        // TODO: reservations change
        // look up based on res code;
        try (Connection conn = DriverManager.getConnection(JDBC_URL,
                JDBC_USER,
                JDBC_PASSWORD)) {
            if (!cmds[1].equals("noChange")) {
                try (PreparedStatement ps = conn.prepareStatement("update lab7_reservations set FirstName=? where CODE = ?;")) {
                    ps.setString(1, cmds[1]);
                    ps.setString(2, cmds[0]);
                    ps.executeUpdate();
                    conn.commit();
                } catch (SQLException e) {
                    System.out.println("First name update Error");
                }
                if (!cmds[2].equals("noChange")) {
                    try (PreparedStatement ps = conn.prepareStatement("update lab7_reservations set LastName=? where CODE = ?;")) {
                        ps.setString(1, cmds[2]);
                        ps.setString(2, cmds[0]);
                        ps.executeUpdate();
                        conn.commit();
                    } catch (SQLException e) {
                        System.out.println("Last name update Error");
                    }
                }
                if (!cmds[3].equals("noChange")) {
                    try (PreparedStatement ps = conn.prepareStatement("update lab7_reservations set CheckIn=? where CODE = ?;")) {
                        ps.setString(1, cmds[3]);
                        ps.setString(2, cmds[0]);
                        ps.executeUpdate();
                        conn.commit();
                    } catch (SQLException e) {
                        System.out.println("Begin of stay update Error");
                    }
                }

                if (!cmds[4].equals("noChange")) {
                    try (PreparedStatement ps = conn.prepareStatement("update lab7_reservations set CheckOut=? where CODE = ?;")) {
                        ps.setString(1, cmds[4]);
                        ps.setString(2, cmds[0]);
                        ps.executeUpdate();
                        conn.commit();
                    } catch (SQLException e) {
                        System.out.println("End of stay update Error");
                    }
                }
                if (!cmds[5].equals("noChange")) {
                    try (PreparedStatement ps = conn.prepareStatement("update lab7_reservations set Kids=? where CODE = ?;")) {
                        ps.setString(1, cmds[5]);
                        ps.setString(2, cmds[0]);
                        ps.executeUpdate();
                        conn.commit();
                    } catch (SQLException e) {
                        System.out.println("Number of children update Error");
                    }
                }
                if (!cmds[6].equals("noChange")) {
                    try (PreparedStatement ps = conn.prepareStatement("update lab7_reservations set Adults=? where CODE = ?;")) {
                        ps.setString(1, cmds[6]);
                        ps.setString(2, cmds[0]);
                        ps.executeUpdate();
                        conn.commit();
                    } catch (SQLException e) {
                        System.out.println("Number of adults update Error");
                    }
                }
            }
        }
    }

    public static void reservationsCancellation() throws SQLException {
        // TODO: access record
        try (Connection conn = DriverManager.getConnection(JDBC_URL,
                JDBC_USER,
                JDBC_PASSWORD)) {

            System.out.println("Enter the reservation code of the reservation you would " +
                    "like to cancel:");

            String s_code = scn.nextLine();
            int code = Integer.parseInt(s_code);
            // code holds reservation code

            // TODO: print reservation
            String sql = "SELECT * FROM lab7_reservations WHERE CODE = " + s_code;

            // Step 3: (omitted in this example) Start transaction

            // Step 4: Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                if (!rs.next())
                {
                    System.out.println("Invalid reservation code");
                }
                else {
                    int Code = rs.getInt("CODE");
                    String Room = rs.getString("Room");
                    String CheckIn = rs.getString("CheckIn");
                    String Checkout = rs.getString("Checkout");
                    float Rate = rs.getFloat("Rate");
                    String LastName = rs.getString("LastName");
                    String FirstName = rs.getString("FirstName");
                    int Adults = rs.getInt("Adults");
                    int Kids = rs.getInt("Kids");
                    System.out.format("\n%-5s %-4s %-10s %-10s %-9s %-15s %-15s %-6s %-4s\n", "CODE", "Room", "CheckIn", "Checkout", "Rate", "LastName", "FirstName", "Adults", "Kids");
                    System.out.format("%-5d %-4s %-10s %-10s ($%.2f) %-15s %-15s %-6d %-4d",
                            Code, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids);
                    System.out.println();

                    System.out.println("type CANCEL to confirm the cancellation of the following reservation:");
                    temp = scn.nextLine();

                    // cancellation condition
                    // Step 5: Receive results
                    if(temp.equals("CANCEL")){
                        // TODO: delete reservation
                        try (PreparedStatement preparedStatement = conn.prepareStatement("DELETE FROM lab7_reservations WHERE Code = ?"))
                        {

                            preparedStatement.setInt(1, code);
                            preparedStatement.execute();
                            conn.commit();
                            System.out.println("Reservation canceled");
                        }
                        catch (SQLException e)
                        {
                            e.printStackTrace();
                            conn.rollback();
                            System.out.println("\nThe reservation either does not exist or could not be found.\n");
                            System.out.println("\nPlease try again.\n");
                        }
                    }
                    // incorrect input for cancellation, returns to main menu
                    else {
                        System.out.println("Incorrect confirmation command");
                        System.out.println("Reservation retained");
                    }
                }
            }
        }
    }

    public static void revenueSummary() throws SQLException {
        String[] months = new String[13];
        float[] values = new float[13];
        months[0] = "January";
        months[1] = "February";
        months[2] = "March";
        months[3] = "April";
        months[4] = "May";
        months[5] = "June";
        months[6] = "July";
        months[7] = "August";
        months[8] = "September";
        months[9] = "October";
        months[10] = "November";
        months[11] = "December";
        months[12] = "Total";

        String sql = "with res as (select Room," +
                "round(sum(case when month(Checkout) = 1 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as January, " +
                "round(sum(case when month(Checkout) = 2 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as February," +
                "round(sum(case when month(Checkout) = 3 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as March, " +
                "round(sum(case when month(Checkout) = 4 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as April, " +
                "round(sum(case when month(Checkout) = 5 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as May, " +
                "round(sum(case when month(Checkout) = 6 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as June, " +
                "round(sum(case when month(Checkout) = 7 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as July, " +
                "round(sum(case when month(Checkout) = 8 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as August, " +
                "round(sum(case when month(Checkout) = 9 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as September, " +
                "round(sum(case when month(Checkout) = 10 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as October, " +
                "round(sum(case when month(Checkout) = 11 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as November, " +
                "round(sum(case when month(Checkout) = 12 then datediff(day, Checkout, CheckIn) *-1 * rate else 0 end),0) as December, " +
                "round(sum(datediff(day, Checkout, Checkin) *-1 * rate),0) as Annual " +
                "from lab7_reservations " +
                "group by Room ) " +
                "select Room, January, February, March, April, May, June, July, August, September, October, November, December, Annual " +
                "from res " +
                "union " +
                "select 'Total', sum(January), sum(February), sum(March), sum(April), sum(May), sum(June), sum(July), sum(August),sum(September), sum(October), sum(November), sum(December), sum(Annual) from res;";

        try (Connection conn = DriverManager.getConnection(JDBC_URL,
                JDBC_USER,
                JDBC_PASSWORD)) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql.toString())) {
                while (rs.next()) {
                    values[0] = rs.getFloat("January");
                    values[1] = rs.getFloat("February");
                    values[2] = rs.getFloat("March");
                    values[3] = rs.getFloat("April");
                    values[4] = rs.getFloat("May");
                    values[5] = rs.getFloat("June");
                    values[6] = rs.getFloat("July");
                    values[7] = rs.getFloat("August");
                    values[8] = rs.getFloat("September");
                    values[9] = rs.getFloat("October");
                    values[10] = rs.getFloat("November");
                    values[11] = rs.getFloat("December");
                    values[12] = rs.getFloat("Annual");
                }
            }

        }
        System.out.println("Annual Revenue Summary");
        System.out.printf("%9s %9s %9s %9s %9s %9s %9s %9s %9s %9s %9s %9s %9s\n",
                months[0],months[1],months[2],months[3],months[4],months[5],months[6],
                months[7],months[8],months[9],months[10],months[11],months[12]);
        System.out.printf("%9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f\n\n",
                values[0],values[1],values[2],values[3],values[4],values[5],values[6],
                values[7],values[8],values[9],values[10],values[11],values[12]);
    }

    public static void printRooms() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL,
                JDBC_USER,
                JDBC_PASSWORD)) {
            // Step 2: Construct SQL statement
            String sql = "SELECT * FROM lab7_rooms";

            // Step 3: (omitted in this example) Start transaction

            // Step 4: Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                // Step 5: Receive results
                while (rs.next()) {
                    String RoomCode = rs.getString("RoomCode");
                    String RoomName = rs.getString("RoomName");
                    int Beds = rs.getInt("Beds");
                    String bedType = rs.getString("bedType");
                    int maxOcc = rs.getInt("maxOcc");
                    float basePrice = rs.getFloat("basePrice");
                    String decor = rs.getString("decor");
                    System.out.format("%s %s %d %s %d ($%.2f) %s",
                            RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor);
                    System.out.println();
                }
                System.out.println();
            }
        }
    }
    public static void printReservations() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL,
                JDBC_USER,
                JDBC_PASSWORD)) {
            // Step 2: Construct SQL statement
            String sql = "SELECT * FROM lab7_reservations";

            // Step 3: (omitted in this example) Start transaction

            // Step 4: Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                // Step 5: Receive results
                while (rs.next()) {
                    int CODE = rs.getInt("CODE");
                    String Room = rs.getString("Room");
                    java.sql.Date CheckIn = rs.getDate("CheckIn");
                    java.sql.Date CheckOut = rs.getDate("CheckOut");
                    float Rate = rs.getFloat("Rate");
                    String LastName = rs.getString("LastName");
                    String FirstName = rs.getString("FirstName");
                    int Adults = rs.getInt("Adults");
                    int Kids = rs.getInt("Kids");
                    System.out.format("%d %s %s %s ($%.2f) %s %s %d %d",
                            CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids);
                    System.out.println();
                }
                System.out.println();
            }
        }
    }

    public static boolean handleCmds() throws SQLException {
        switch (cmds[0]){
            case "1":
                roomAndRates();
                break;
            case "2":
                reservations();
                break;
            case "3":
                reservationsChange();
                break;
            case "4":
                reservationsCancellation();
                break;
            case "5":
                revenueSummary();
                break;
            /*case "6":
                printReservations();
                break;*/
            case "0":
                return true;
            default:
                System.out.println("Invalid Command\n");
        }
        return false;
    }
}

