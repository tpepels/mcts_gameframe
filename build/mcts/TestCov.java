import ai.Covariance;

import java.util.Scanner;

public class TestCov {
    public static void main(String[] args) {
        Covariance cov = new Covariance();
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        while (!input.equals(".")) {
            String[] arr = input.split(" ");
            cov.push(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]));
            input = scanner.nextLine();
        }
        System.out.println("Mean1: " + cov.getMean1());
        System.out.println("Mean2: " + cov.getMean2());
        System.out.println("Variance1: " + cov.variance1());
        System.out.println("Variance2: " + cov.variance2());
        System.out.println("N: " + cov.getN());
        System.out.println("Covariance: " + cov.getCovariance());
        System.out.println("a': " + cov.getCovariance() / cov.variance2());
    }
}
