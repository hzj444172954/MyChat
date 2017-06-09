package com.util;

import java.sql.*;

public class Conn {

	private static String jdbcName = "com.mysql.jdbc.Driver";
	private static String url = "jdbc:mysql://localhost:3306/mychat";
	private static String user = "root";
	private static String password = "123";

	/**
	 * ��ȡ���ݿ�����
	 * 
	 * @return��ȡ�ض����ݿ�����
	 */
	public static Connection getConnection() {
		try {
			Class.forName(jdbcName);
		} catch (ClassNotFoundException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
		try {
			Connection aConnection = DriverManager.getConnection(url, user,
					password);
			return aConnection;
		} catch (SQLException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * �ر����ݿ�����
	 * 
	 * @param ���ر����ݿ�����
	 */

	public static void closeConnection(Connection aConnection) {
		try {
			if (aConnection != null) {
				aConnection.close();
			}
		} catch (SQLException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Connection aConnection = Conn.getConnection();
		Conn.closeConnection(aConnection);
		System.out.println("���ӳɹ�");
	}
}
