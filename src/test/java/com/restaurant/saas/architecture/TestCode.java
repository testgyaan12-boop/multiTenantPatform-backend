package com.restaurant.saas.architecture;

public class TestCode {
	public static void main(String[] args) {

		int[][] a = { { 2, 5, 6 }, { 7, 25, 69, 56 } };

		System.out.println("length of a: " + a.length);

		for (int i = 0; i < a.length; i++) {

			System.out.println("length of a[i]: " + a[i].length);

			for (int j = 0; j < a[i].length; j++) {

				System.out.println("index:" + i + j + ":value:" + a[i][j]);
			}
		}
	}  
}
