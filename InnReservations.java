import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Scanner;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/*
 * Introductory JDBC examples based loosely on the BAKERY dataset from CSC 365 labs.
 */
public class InnReservations {

    private final String JDBC_URL = "jdbc:h2:~/csc365_lab7";
    private final String JDBC_USER = "";
    private final String JDBC_PASSWORD = "";
    private static boolean exit = false;
    
    public static void main(String[] args) {
	try {
	    InnReservations hp = new InnReservations();
            hp.initDb();
            //hp.demo2();
		while(!exit){
			UI.getUserInput();
			exit = UI.handleCmds();
		}
	} catch (SQLException e) {
	    System.err.println("SQLException: " + e.getMessage());
	}
    }
    

    private void initDb() throws SQLException {
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    try (Statement stmt = conn.createStatement()) {
				stmt.execute("DROP TABLE IF EXISTS lab7_reservations");
                stmt.execute("DROP TABLE IF EXISTS lab7_rooms");
                stmt.execute("CREATE TABLE lab7_rooms (RoomCode char(5) PRIMARY KEY, " +
						"RoomName varchar(30), " +
						"Beds int, " +
						"bedType varchar(8), " +
						"maxOcc int, " +
						"basePrice decimal(7,2), " +
						"decor varchar(20), " +
						"unique(RoomName))");

			//populate lab7_rooms
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('AOB', 'Abscond or bolster', 2, 'Queen', 4, 175, 'traditional')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('CAS', 'Convoke and sanguine', 2, 'King', 4, 175, 'traditional')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('FNA', 'Frugal not apropos', 2, 'King', 4, 250, 'traditional')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('HBB', 'Harbinger but bequest', 1, 'Queen', 2, 100, 'modern')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('IBD', 'Immutable before decorum', 2, 'Queen', 4, 150, 'rustic')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('IBS', 'Interim but salutary', 1, 'King', 2, 150, 'traditional')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('MWC', 'Mendicant with cryptic', 2, 'Double', 4, 125, 'modern')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('RND', 'Recluse and defiance', 1, 'King', 2, 150, 'modern')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('RTE', 'Riddle to exculpate', 2, 'Queen', 4, 175, 'rustic')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('SAY', 'Stay all year', 1, 'Queen', 3, 100, 'modern')");
			stmt.execute("INSERT INTO lab7_rooms(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('TAA', 'Thrift and accolade', 1, 'Double', 2, 75, 'modern')");

				stmt.execute("CREATE TABLE lab7_reservations ("+
					"CODE int(11) PRIMARY KEY, " +
					"Room char(5), " +
					"CheckIn date, " +
					"Checkout date, " +
					"Rate decimal(7,2), " +
					"LastName varchar(15), " +
					"FirstName varchar(15), " +
					"Adults int(11), " +
					"Kids int(11)," +
					"foreign key (room) references lab7_rooms(RoomCode))");

			//populate lab7_reservations

			stmt.execute("INSERT INTO lab7_reservations(CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (10105, 'HBB', '2010-10-23', '2010-10-25', 100, 'SELBIG', 'CONRAD', 1, 0);");
			stmt.execute("INSERT INTO lab7_reservations(CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (10183, 'IBD', '2010-09-19', '2010-09-20', 150, 'GABLER', 'DOLLIE', 2, 0);");
			stmt.execute("INSERT INTO lab7_reservations(CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (10449, 'RND', '2010-09-30', '2010-10-01', 150, 'KLESS', 'NELSON', 1, 0);");
			stmt.execute("INSERT INTO lab7_reservations(CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (10489, 'AOB', '2010-02-02', '2010-02-05', 218.75, 'CARISTO', 'MARKITA', 2, 1);");
			stmt.execute("INSERT INTO lab7_reservations(CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (10500, 'HBB', '2010-08-11', '2010-08-12', 90, 'YESSIOS', 'ANNIS', 1, 0);");
			stmt.execute("INSERT INTO lab7_reservations(CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (10574, 'FNA', '2010-11-26', '2010-12-03', 287.5, 'SWEAZY', 'ROY', 2, 1);");
			stmt.execute("INSERT INTO lab7_reservations(CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (10984, 'AOB', '2010-12-28', '2011-01-01', 201.25, 'ZULLO', 'WILLY', 2, 1);");

	    }
	}
    }
}

