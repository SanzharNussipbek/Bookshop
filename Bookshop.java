/*
 * COMP2016 Database Management
 * Semester 2, 2019-2020
 * Group #7: Sanzhar Nussipbek 18200257
 *           Gaziz Zhumash 18200249
 *           Anthony David Stoltzfus 18208568

 *
 * "UNIVERSITY BOOKSHOP PROJECT"
 * 
 * Need to change the username and password in loginDB() function to use the program
 */


/* Importing all necessary modules for Java and JDBC */
import java.awt.GridLayout;
import java.awt.TextField;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import javax.swing.*;

import java.util.Properties;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


/* Bookshop class */
public class Bookshop{
    Scanner in = null;
	Connection conn = null;
	// Database Host
	final String databaseHost = "orasrv1.comp.hkbu.edu.hk";
	// Database Port
	final int databasePort = 1521;
	// Database name
	final String database = "pdborcl.orasrv1.comp.hkbu.edu.hk";
	final String proxyHost = "faith.comp.hkbu.edu.hk";
	final int proxyPort = 22;
	final String forwardHost = "localhost";
	int forwardPort;
	Session proxySession = null;
	boolean noException = true;

	// JDBC connecting host
	String jdbcHost;
	// JDBC connecting port
    int jdbcPort;


	//Array of options
	String[] options = {"show all tables",
						"make an order",
						"cancel an order (by order id or student id)",
						"search orders (by student id)",
						"update an order (by order_id)",
						"add new student",
						"add new book",
						"exit"};
	
	//Array of table names
	String[] tables = new String[]{"STUDENT", "BOOK", "ORDER_ITEM", "ORDERS"};
	
	//Array of tables' headings
	String[][] heads = {{"Student ID", "Name", "Gender", "Major", "Discount(%)"}, //STUDENT table
						{"Book ID", "Title", "Author", "Price", "Stock"},	//BOOK table
						{"Order ID", "Book ID", "Quantity", "Price", "Address", "E-mail", "Order status", "Delivery date"}, //ORDER_ITEM Table
						{"Order ID", "Student ID", "Total price", "Payment method", "Card number", "Order date"}};	//ORDERS table


	//Bookshop class constructor					
	public Bookshop() {
		System.out.println("Welcome to the University Bookshop!");
		in = new Scanner(System.in);
	}

	/**
	 * Main function
	 * @param args
	 */
	public static void main(String[] args) {
		Bookshop bookshop = new Bookshop(); //Create an instance of the class

		if (!bookshop.loginProxy()) {	//Logging in 
			System.out.println("Login proxy failed, please re-examine your username and password!");
			return;
		}
		if (!bookshop.loginDB()) {	//Logging in
			System.out.println("Login database failed, please re-examine your username and password!");
			return;
		}
		System.out.println("Login succeed!");

		try {
			bookshop.run();		//run the program and connection
		} finally {
			bookshop.close();		//close the program and connection
		}
	}


	/**
	 * Run function
	 * Displays choices in the console and receives a number 
	 * of the choice and calls other functions respectively
	 */
	public void run() {
		while (noException) {
			showOptions();
			String line = in.nextLine();
			if (line.equalsIgnoreCase("exit"))
				return;
			int choice = -1;
			try {
				choice = Integer.parseInt(line);
			} catch (Exception e) {
				System.out.println("This option is not available");
				continue;
			}

			if (!(choice >= 1 && choice <= options.length)) {
				System.out.println("This option is not available");
				continue;
			}

			if (options[choice - 1].equals("show all tables")) {
				showTables();
			} else if (options[choice - 1].equals("make an order")) {
				addOrder();
			} else if (options[choice - 1].equals("cancel an order (by order id or student id)")) {
				deleteOrders();
			} else if (options[choice - 1].equals("search orders (by student id)")) {
				searchOrders();
			} else if (options[choice - 1].equals("update an order (by order_id)")) {
				updateOrder();
			} else if(options[choice - 1].equals("add new student")){
				addStudent();
			} else if(options[choice - 1].equals("add new book")){
				addBook();
			}else if (options[choice - 1].equals("exit")) {
				break;
			}
		}
	}



	/**
	 * Close function
	 * Closes the connection and the program
	 */
	public void close() {
		System.out.println("Thanks for using this University Bookshop! Bye...");
		try {
			if (conn != null)
				conn.close();
			if (proxySession != null) {
				proxySession.disconnect();
			}
			in.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Function to display the options in the console
	 */
	public void showOptions() {
		System.out.println("Please choose one of the following options:");
		for (int i = 0; i < options.length; ++i) {
			System.out.println("(" + (i + 1) + ") " + options[i]);
		}
	}


	/**
	 * Function to query all the tables form the database and display them
	 */
	private void showTables(){
		for(int i = 0; i < 4; i++) { 	//Print all 4 tables
			System.out.println("\n" + tables[i] + " TABLE:");				//Print table name
			System.out.println("======================================");
			try {
				Statement stm = conn.createStatement();
				String sql = "SELECT * FROM " + tables[i];					//Query statement
				ResultSet rs = stm.executeQuery(sql);
				
				while(rs.next()){
					for (int j = 0; j < heads[i].length; ++j) { 
						try {
							System.out.printf("%-15s: %s\n", heads[i][j], rs.getString(j + 1));		//Print all the rows
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
					System.out.println("\n----------------------------------------\n");
				}
			} catch (SQLException e) {
				e.printStackTrace();
				noException = false;
			}
		}
	}



	/**
	 * Function to search orders by student id in the database
	 */
	private void searchOrders() {
		System.out.println("Please input the Student ID to search orders:");

		String line = in.nextLine();
		line = line.trim();

		if (line.equalsIgnoreCase("exit"))
			return;

		printOrderInfo(line);
	}


	/**
	 * Function to query orders from the database with sid and display them
	 * @param sid
	 */
	private void printOrderInfo(String sid) {
		try {
			Statement stm = conn.createStatement();
			String sql = "SELECT * FROM ORDER_ITEM WHERE ORDER_ID IN (SELECT ORDER_ID FROM ORDERS WHERE SID = " + sid + ")";
			ResultSet rs = stm.executeQuery(sql);
			System.out.println("Each orders of the student with SID = " + sid + " in the ORDER_ITEM table:");
			System.out.println("==========================================================================");
			while(rs.next()){
				for (int i = 0; i < 8; ++i) { // order_item table has 8 attributes
					try {
						System.out.printf("%-15s: %s\n", heads[2][i], rs.getString(i + 1));
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				System.out.println("\n----------------------------------------\n");
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			noException = false;
		}
	}


	/**
	 * Universal function to prompt for input.
	 * Accepts a text which should be displayed with the template
	 * Accepts the exact number of inputs which the array of values should have
	 * Returns array of values the user inserted
	 * @param text
	 * @param input_num
	 * @return
	 */
	private String[] getInput(String text, int input_num){
		System.out.println("Please input " + text + ":");
		String line = in.nextLine();
		
		if (line.equalsIgnoreCase("exit")){
			System.exit(0);
		}

		String[] values = line.split(",");

		if (values.length < input_num) {
			System.out.println("The number of inputs is expected to be " + input_num);
			System.exit(0);
		}

		for (int i = 0; i < values.length; ++i){
			values[i] = values[i].trim();
		}

		return values;
	}



	/**
	 * Function to add a book to the database
	 */
	private void addBook(){
		String[] book_values = getInput("book title, author, price, stock", 4); //Asks for input of necessary atributes
		String title = book_values[0];
		String author = book_values[1];
		double price = Double.parseDouble(book_values[2]);					//convert price value to double
		int stock = Integer.parseInt(book_values[3]);						//convert stock value to integer

		int bid = 0;
		try{
			Statement stm = conn.createStatement();
			String sql = "SELECT MAX(BID) FROM BOOK";						//Query the largest book id
			ResultSet rs = stm.executeQuery(sql);
			if (!rs.next())
				return;
			bid = rs.getInt(1)+1;											//Set the book id of the input to the next largest
			stm.close();
		}catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
		try {
			Statement stm = conn.createStatement();	
			String sql = "INSERT INTO BOOK VALUES(" + bid + ", '"			//Insert values to the table BOOK
													+ title + "', '"
													+ author + "', "
													+ price + ", "
													+ stock + ")";
			stm.executeUpdate(sql);
			stm.close();
			System.out.println("The book record is successfully added.");
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
	}



	/**
	 * Function to add a student to the database
	 */
	private void addStudent(){
		String[] student_values = getInput("student name, gender(M or F), major", 3);
		String name = student_values[0];
		String gender = student_values[1];
		String major = student_values[2];

		int sid = 0;
		try{
		Statement stm = conn.createStatement();
		String sql = "SELECT MAX(SID) FROM STUDENT";
		ResultSet rs = stm.executeQuery(sql);
		if (!rs.next())
			return;
		sid = rs.getInt(1)+1;
		stm.close();
		}catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
		try {
			Statement stm = conn.createStatement();
			String sql = "INSERT INTO STUDENT(SID, NAME, GENDER, MAJOR) VALUES(" + sid + ", '"
																				+ name + "', '"
																				+ gender + "', '"
																				+ major + "')";
			stm.executeUpdate(sql);
			stm.close();
			System.out.println("The student record is successfully added.");
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
	}



	/**
	 * Function to add a new order to the database
	 */
	private void addOrder() {
		/**
		 * First need to insert new row into ORDERS table: ORDER_ID, SID, PAYMENT_METHOD, CARD_NO
		 * Then, need to insert new rows into ORDER_ITEM table for each book: BID, QUANTITY, PRICE, ADDRESS, EMAIL
		 */

		String[] order_values = getInput("student ID and payment method(cash or card)", 2);		//asks for sid and payment method


		String card_no = "";

		if(order_values[1].equalsIgnoreCase("card")){			//if payment method is "card" asks for card number
			card_no = getInput("card number(16 digits)", 1)[0];
		}

		//Asks for number of unique books to know how many rows to insert into ORDER_ITEM table
		int total_book_num = Integer.parseInt(getInput("number of unique books(how many different books)", 1)[0]);	
		String order_id = "";

		try {
			Statement stm = conn.createStatement();
			String sql = "SELECT MAX(ORDER_ID) FROM ORDERS";		//query the largest order id from the db
			ResultSet rs = stm.executeQuery(sql);
			if (!rs.next())
				return;
			order_id = Integer.toString(rs.getInt(1)+1);			//set the next order id
			stm.close();
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}


		//First insert into ORDERS table
		try {
			Statement stm = conn.createStatement();
			String sql = "INSERT INTO ORDERS(ORDER_ID, SID, PAYMENT_METHOD, CARD_NO) VALUES('" + order_id + "', " 		//this is order_id
																						      + order_values[0] + ", '"  //this is sid
																							  + order_values[1] + "', '" 	//this is payment_method
																							  + card_no +"')";			//this is card_no
			stm.executeUpdate(sql);
			stm.close();
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}

		//Then insert into ORDER_ITEM table
		String[][] order_items = new String[total_book_num][]; //Array of unique books' values
		for(int i = 0; i < total_book_num; i++){
			order_items[i] = getInput("book id, quantity of this book, address and email", 4); //Prompt for order item values
		}


		for(int i = 0; i < total_book_num; i++){
			try {
				Statement stm = conn.createStatement();
				String sql = "SELECT PRICE FROM BOOK WHERE BID = " + order_items[i][0];		//get the price from the BOOK table
				ResultSet rs = stm.executeQuery(sql);
				if (!rs.next())
					return;
				
				String price = rs.getString(1);

				sql = "INSERT INTO ORDER_ITEM(ORDER_ID, BID, QUANTITY, PRICE, ADDRESS, EMAIL) VALUES('" + order_id + "', " //this is order_id
																											  + order_items[i][0] + ", " //this is bid
																											  + order_items[i][1] + ", '" //this is quantity
																											  + price + "', '" //this is price
																											  + order_items[i][2] + "', '" //this is address
																											  + order_items[i][3] + "')"; //this is email
				stm.executeUpdate(sql);
				stm.close();
			} catch (SQLException e) {
				e.printStackTrace();
				noException = false;
			}
		}
	}


	/**
	 * Function to delete orders from db
	 * First prompts for an attribute name
	 */
	private void deleteOrders(){
		String by = getInput("what attribute you want to use(student id or order id)", 1)[0];

		if(by.equalsIgnoreCase("student id")) deleteOrdersBySID();	//Call function to delete orders by student id
		else if(by.equalsIgnoreCase("order id")) deleteOrdersByOrderID();	//Call function to delete orders by order id
		else {
			System.out.println("Invalid input!");
			return;
		}
	}



	/**
	 * Function to delete orders by student id
	 */
	private void deleteOrdersBySID(){
		int sid = Integer.parseInt(getInput("student id", 1)[0]);	//prompt for student id

		try {
			Statement stm = conn.createStatement();
			String sql = "DELETE FROM ORDERS WHERE SID = " + sid ;	//sql statement
			stm.executeUpdate(sql);
			stm.close();
			System.out.println("All orders of the student with SID = " + sid + " are deleted successfully!");
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
	}



	/**
	 * Function to delete orders by order id
	 */
	private void deleteOrdersByOrderID(){
		String order_id = getInput("order id", 1)[0]; //prompt for order id

		try {
			Statement stm = conn.createStatement();
			String sql = "DELETE FROM ORDERS WHERE ORDER_ID = '" + order_id + "'";
			stm.executeUpdate(sql);
			stm.close();
			System.out.println("All orders of with ORDER_ID = " + order_id + " are deleted successfully!");
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
	}



	/**
	 * Function to update orders 
	 * First ask for specific attribute name 
	 */
	private void updateOrder(){
		String by = getInput("what attribute you want to use(student id or order id)", 1)[0];

		if(by.equalsIgnoreCase("student id")) updateOrderBySID();	//Call function to update orders by student id
		else if(by.equalsIgnoreCase("order id")) updateOrderByOrderID();	//Call function to update orders by order id
		else {
			System.out.println("Invalid input!");
			return;
		}
	}



	/**
	 * Function to update orders by student id
	 */
	private void updateOrderBySID(){
		int sid = Integer.parseInt(getInput("student id", 1)[0]);

		try {
			Statement stm = conn.createStatement();
			String sql = "UPDATE ORDER_ITEM SET ORDER_STATUS = 'DELIVERED' WHERE ORDER_ID IN (SELECT ORDER_ID FROM ORDERS WHERE SID = " + sid + ")";
			stm.executeUpdate(sql);
			stm.close();
			System.out.println("All orders of the student with SID = " + sid + " are updated successfully!");
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
	}


	/**
	 * Function to update orders by order id
	 */	
	private void updateOrderByOrderID(){
		int order_id = Integer.parseInt(getInput("order id", 1)[0]);

		try {
			Statement stm = conn.createStatement();
			String sql = "UPDATE ORDER_ITEM SET ORDER_STATUS = 'DELIVERED' WHERE ORDER_ID = '" + order_id + "'" ;
			stm.executeUpdate(sql);
			stm.close();
			System.out.println("All orders of the student with order id = " + order_id + " are updated successfully!");
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
	}





	/**
	 * Functions needed for JDBC and connection with the database
	 * @param message
	 * @return
	 */
	boolean getYESorNO(String message) {
		JPanel panel = new JPanel();
		panel.add(new JLabel(message));
		JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
		JDialog dialog = pane.createDialog(null, "Question");
		dialog.setVisible(true);
		boolean result = JOptionPane.YES_OPTION == (int) pane.getValue();
		dialog.dispose();
		return result;
	}


	String[] getUsernamePassword(String title) {
		JPanel panel = new JPanel();
		final TextField usernameField = new TextField();
		final JPasswordField passwordField = new JPasswordField();
		panel.setLayout(new GridLayout(2, 2));
		panel.add(new JLabel("Username"));
		panel.add(usernameField);
		panel.add(new JLabel("Password"));
		panel.add(passwordField);
		JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
			private static final long serialVersionUID = 1L;

			@Override
			public void selectInitialValue() {
				usernameField.requestFocusInWindow();
			}
		};
		JDialog dialog = pane.createDialog(null, title);
		dialog.setVisible(true);
		dialog.dispose();
		return new String[] { usernameField.getText(), new String(passwordField.getPassword()) };
	}


	public boolean loginProxy() {
		if (getYESorNO("Using ssh tunnel or not?")) { // if using ssh tunnel
			String[] namePwd = getUsernamePassword("Login cs lab computer");
			String sshUser = namePwd[0];
			String sshPwd = namePwd[1];
			try {
				proxySession = new JSch().getSession(sshUser, proxyHost, proxyPort);
				proxySession.setPassword(sshPwd);
				Properties config = new Properties();
				config.put("StrictHostKeyChecking", "no");
				proxySession.setConfig(config);
				proxySession.connect();
				proxySession.setPortForwardingL(forwardHost, 0, databaseHost, databasePort);
				forwardPort = Integer.parseInt(proxySession.getPortForwardingL()[0].split(":")[0]);
			} catch (JSchException e) {
				e.printStackTrace();
				return false;
			}
			jdbcHost = forwardHost;
			jdbcPort = forwardPort;
		} else {
			jdbcHost = databaseHost;
			jdbcPort = databasePort;
		}
		return true;
	}



	/**
	 * Need to change the username and password to use the program
	 * @return
	 */
	public boolean loginDB() {
		String username = "e8200257";//Replace e1234567 to your username
		String password = "e8200257";//Replace e1234567 to your password
		
		/* Do not change the code below */
		if(username.equalsIgnoreCase("e1234567") || password.equalsIgnoreCase("e1234567")) {
			String[] namePwd = getUsernamePassword("Login sqlplus");
			username = namePwd[0];
			password = namePwd[1];
		}
		String URL = "jdbc:oracle:thin:@" + jdbcHost + ":" + jdbcPort + "/" + database;

		try {
			System.out.println("Logging " + URL + " ...");
			conn = DriverManager.getConnection(URL, username, password);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

}