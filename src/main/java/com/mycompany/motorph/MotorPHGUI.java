/*
 * MotorPHGUI class with proper hour calculations
 */
package com.mycompany.motorph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.List;
import java.text.DecimalFormat;

public class MotorPHGUI {

    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private static JFrame mainFrame;
    private static CardLayout cardLayout;
    private static JPanel cardPanel;

    // Helper record for pay coverage period
    private record PayCoverage(int year, int month, int week) {

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Load attendance records first
                loadAttendanceRecords();

                // Create and show the GUI
                createAndShowGUI();
            } catch (Exception e) {
                showErrorDialog("A critical error occurred: " + e.getMessage());
                System.exit(1);
            }
        });
    }

    private static void loadAttendanceRecords() {
        final int MAX_RETRIES = 3;
        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {
                AttendanceRecord.loadAttendanceFromCSV("src/main/resources/AttendanceRecord.csv");
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    showErrorDialog("Failed to load attendance records after " + MAX_RETRIES + " attempts.");
                    System.exit(1);
                }
            }
        }
    }

    private static void createAndShowGUI() {
        // Create main frame
        mainFrame = new JFrame("MotorPH Payroll System");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);

        // Create card layout for different views
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Create panels
        cardPanel.add(createMenuPanel(), "Menu");
        cardPanel.add(createEmployeeDetailsPanel(), "EmployeeDetails");
        cardPanel.add(createGrossWagePanel(), "GrossWage");
        cardPanel.add(createNetWagePanel(), "NetWage");

        // Add card panel to frame
        mainFrame.add(cardPanel, BorderLayout.CENTER);

        // Center the frame on screen
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("MotorPH Payroll System", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(headerLabel, BorderLayout.NORTH);

        // Menu buttons
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        JButton employeeDetailsBtn = new JButton("Employee Details");
        employeeDetailsBtn.addActionListener(e -> cardLayout.show(cardPanel, "EmployeeDetails"));

        JButton grossWageBtn = new JButton("Calculate Gross Wage");
        grossWageBtn.addActionListener(e -> cardLayout.show(cardPanel, "GrossWage"));

        JButton netWageBtn = new JButton("Calculate Net Wage");
        netWageBtn.addActionListener(e -> cardLayout.show(cardPanel, "NetWage"));

        JButton exitBtn = new JButton("Exit");
        exitBtn.addActionListener(e -> System.exit(0));

        buttonPanel.add(employeeDetailsBtn);
        buttonPanel.add(grossWageBtn);
        buttonPanel.add(netWageBtn);
        buttonPanel.add(exitBtn);

        panel.add(buttonPanel, BorderLayout.CENTER);

        return panel;
    }

    private static JPanel createEmployeeDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("Employee Details", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Back button
        JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "Menu"));
        headerPanel.add(backButton, BorderLayout.WEST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Content
        JTabbedPane tabbedPane = new JTabbedPane();

        // Individual Employee Tab
        JPanel individualPanel = new JPanel(new BorderLayout());
        individualPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JLabel searchLabel = new JLabel("Employee ID:");
        JTextField searchField = new JTextField(15);
        JButton searchButton = new JButton("Search");

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(resultArea);

        searchButton.addActionListener(e -> {
            try {
                String empId = searchField.getText().trim();
                validateEmployeeId(empId);

                Employee employee = findEmployeeById(empId);
                if (employee == null) {
                    throw new IllegalArgumentException("Employee with ID " + empId + " not found");
                }

                resultArea.setText(employee.toString());
            } catch (Exception ex) {
                showErrorDialog(ex.getMessage());
            }
        });

        individualPanel.add(searchPanel, BorderLayout.NORTH);
        individualPanel.add(scrollPane, BorderLayout.CENTER);

        // All Employees Tab
        JPanel allEmployeesPanel = new JPanel(new BorderLayout());
        allEmployeesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea allEmployeesArea = new JTextArea();
        allEmployeesArea.setEditable(false);
        allEmployeesArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane allEmployeesScroll = new JScrollPane(allEmployeesArea);

        JButton refreshButton = new JButton("Refresh List");
        refreshButton.addActionListener(e -> {
            try {
                List<Employee> employees = EmployeeModelFromFile.getEmployeeModelList();
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%-10s %-20s %-20s%n", "ID", "Last Name", "First Name"));
                sb.append("------------------------------------------------\n");

                for (Employee employee : employees) {
                    sb.append(String.format("%-10s %-20s %-20s%n",
                            employee.getEmployeeNumber(),
                            employee.getLastName(),
                            employee.getFirstName()));
                }

                allEmployeesArea.setText(sb.toString());
            } catch (Exception ex) {
                showErrorDialog("Error listing employees: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(refreshButton);
        allEmployeesPanel.add(buttonPanel, BorderLayout.NORTH);
        allEmployeesPanel.add(allEmployeesScroll, BorderLayout.CENTER);

        // Add tabs
        tabbedPane.addTab("Individual Employee", individualPanel);
        tabbedPane.addTab("All Employees", allEmployeesPanel);
        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }

    private static JPanel createGrossWagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("Gross Wage Calculation", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Back button
        JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "Menu"));
        headerPanel.add(backButton, BorderLayout.WEST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Employee ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Employee ID:"), gbc);

        gbc.gridx = 1;
        JTextField empIdField = new JTextField(15);
        inputPanel.add(empIdField, gbc);

        // Year
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Year (YYYY):"), gbc);

       gbc.gridx = 1;
JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(
    LocalDate.now().getYear(), 2000, LocalDate.now().getYear() + 1, 1));
// Remove comma formatting
JSpinner.NumberEditor editor = new JSpinner.NumberEditor(yearSpinner, "#");
yearSpinner.setEditor(editor);
inputPanel.add(yearSpinner, gbc);

        // Month
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Month (1-12):"), gbc);

        gbc.gridx = 1;
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        inputPanel.add(monthSpinner, gbc);

        // Week
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("Week (1-4):"), gbc);

        gbc.gridx = 1;
        JSpinner weekSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        inputPanel.add(weekSpinner, gbc);

        // Calculate Button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        JButton calculateButton = new JButton("Calculate Gross Wage");
        inputPanel.add(calculateButton, gbc);

        // Results Panel
        JTextArea resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(resultsArea);

        calculateButton.addActionListener(e -> {
            try {
                String empId = empIdField.getText().trim();
                validateEmployeeId(empId);

                int year = (int) yearSpinner.getValue();
                int month = (int) monthSpinner.getValue();
                int week = (int) weekSpinner.getValue();

                validatePayPeriod(year, month, week);

                // Refresh attendance data
                AttendanceRecord.loadAttendanceFromCSV("src/main/resources/AttendanceRecord.csv");

                Employee employee = findEmployeeById(empId);
                if (employee == null) {
                    throw new IllegalArgumentException("Employee with ID " + empId + " not found");
                }

                // Create and calculate gross wage
                Grosswage grosswage = new Grosswage(
                        empId,
                        employee.getFirstName(),
                        employee.getLastName(),
                        year,
                        month,
                        week,
                        employee.getShiftStartTime(),
                        employee.isNightShift()
                );

                // Calculate and display results
                grosswage.calculate();
                resultsArea.setText(formatGrossWageDetails(week, month, year, grosswage));

                // Debug output
                System.out.println("Regular Hours: " + grosswage.getRegularHours());
                System.out.println("Overtime Hours: " + grosswage.getOvertimeHours());
            } catch (Exception ex) {
                showErrorDialog("Error calculating gross wage: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Add components to main panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(inputPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private static JPanel createNetWagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("Net Wage Calculation", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Back button
        JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "Menu"));
        headerPanel.add(backButton, BorderLayout.WEST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Employee ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Employee ID:"), gbc);

        gbc.gridx = 1;
        JTextField empIdField = new JTextField(15);
        inputPanel.add(empIdField, gbc);

        // Year
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Year (YYYY):"), gbc);

        gbc.gridx = 1;
JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(
    LocalDate.now().getYear(), 2000, LocalDate.now().getYear() + 1, 1));
// Remove comma formatting
JSpinner.NumberEditor editor = new JSpinner.NumberEditor(yearSpinner, "#");
yearSpinner.setEditor(editor);
inputPanel.add(yearSpinner, gbc);

        // Month
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Month (1-12):"), gbc);

        gbc.gridx = 1;
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        inputPanel.add(monthSpinner, gbc);

        // Week
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("Week (1-4):"), gbc);

        gbc.gridx = 1;
        JSpinner weekSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        inputPanel.add(weekSpinner, gbc);

        // Calculate Button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        JButton calculateButton = new JButton("Calculate Net Wage");
        inputPanel.add(calculateButton, gbc);

        // Results Panel
        JTextArea resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(resultsArea);

        calculateButton.addActionListener(e -> {
            try {
                String empId = empIdField.getText().trim();
                validateEmployeeId(empId);

                int year = (int) yearSpinner.getValue();
                int month = (int) monthSpinner.getValue();
                int week = (int) weekSpinner.getValue();

                validatePayPeriod(year, month, week);

                // Refresh attendance data
                AttendanceRecord.loadAttendanceFromCSV("src/main/resources/AttendanceRecord.csv");

                Employee employee = findEmployeeById(empId);
                if (employee == null) {
                    throw new IllegalArgumentException("Employee with ID " + empId + " not found");
                }

                // Create and calculate gross wage
                Grosswage grosswage = new Grosswage(
                        empId,
                        employee.getFirstName(),
                        employee.getLastName(),
                        year,
                        month,
                        week,
                        employee.getShiftStartTime(),
                        employee.isNightShift()
                );

                double gross = grosswage.calculate();
                String employeeName = employee.getLastName() + ", " + employee.getFirstName();

                Netwage netwage = new Netwage(
                        empId,
                        employeeName,
                        gross,
                        grosswage.getHoursWorked(),
                        week,
                        grosswage,
                        month,
                        year
                );

                resultsArea.setText(formatPayrollResults(week, month, year, empId, employeeName, grosswage, netwage));
            } catch (Exception ex) {
                showErrorDialog("Error calculating net wage: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Add components to main panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(inputPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    // ================== VALIDATION METHODS ================== //
    private static void validateEmployeeId(String empId) throws IllegalArgumentException {
        if (empId.isEmpty()) {
            throw new IllegalArgumentException("Please enter an Employee ID");
        }

        if (!empId.matches("\\d+")) {
            throw new IllegalArgumentException("Employee ID must contain only numbers");
        }
    }

    private static void validatePayPeriod(int year, int month, int week) throws IllegalArgumentException {
        LocalDate currentDate = LocalDate.now();
        if (year > currentDate.getYear() + 1) {
            throw new IllegalArgumentException("Year cannot be more than 1 year in the future");
        }
    }

    // ================== FORMATTING METHODS ================== //
    private static String formatGrossWageDetails(int week, int month, int year, Grosswage grosswage) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== GROSS WAGE DETAILS ===%n"));
        sb.append(String.format("Week %d, Month %d/%d%n", week, month, year));
        sb.append(String.format("---------------------------%n"));
        sb.append(String.format("%-25s: %s hrs%n", "Regular Hours", decimalFormat.format(grosswage.getRegularHours())));
        sb.append(String.format("%-25s: %s hrs%n", "Overtime Hours", decimalFormat.format(grosswage.getOvertimeHours())));
        sb.append(String.format("%-25s: PHP %s%n", "Regular Pay", decimalFormat.format(grosswage.getRegularPay())));
        sb.append(String.format("%-25s: PHP %s%n", "Overtime Pay", decimalFormat.format(grosswage.getOvertimePay())));
        sb.append(String.format("%-25s: PHP %s%n", "Holiday Premium Pay", decimalFormat.format(grosswage.getHolidayPay())));
        sb.append(String.format("%-25s: PHP %s%n", "Total Gross Wage", decimalFormat.format(grosswage.calculate())));

        return sb.toString();
    }

    private static String formatPayrollResults(int week, int month, int year, String empId,
            String employeeName, Grosswage grosswage, Netwage netwage) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== PAYROLL RESULTS ===%n"));
        sb.append(String.format("Week %d, Month %d/%d%n", week, month, year));
        sb.append(String.format("-----------------------%n"));
        sb.append(String.format("%-20s: %s%n", "Employee ID", empId));
        sb.append(String.format("%-20s: %s%n", "Employee Name", employeeName));
        sb.append(String.format("-----------------------%n"));
        sb.append(String.format("%-20s: %s hrs%n", "Regular Hours", decimalFormat.format(grosswage.getRegularHours())));
        sb.append(String.format("%-20s: %s hrs%n", "Overtime Hours", decimalFormat.format(grosswage.getOvertimeHours())));
        sb.append(String.format("%-20s: PHP %s%n", "Regular Pay", decimalFormat.format(grosswage.getRegularPay())));
        sb.append(String.format("%-20s: PHP %s%n", "Overtime Pay", decimalFormat.format(grosswage.getOvertimePay())));
        sb.append(String.format("%-20s: PHP %s%n", "Gross Wage", decimalFormat.format(grosswage.calculate())));

        sb.append(String.format("%nDeductions:%n"));
        sb.append(String.format("%-20s: PHP %s%n", "SSS", decimalFormat.format(netwage.getSSSDeduction())));
        sb.append(String.format("%-20s: PHP %s%n", "PhilHealth", decimalFormat.format(netwage.getPhilhealthDeduction())));
        sb.append(String.format("%-20s: PHP %s%n", "Pag-IBIG", decimalFormat.format(netwage.getPagIbigDeduction())));
        sb.append(String.format("%-20s: PHP %s%n", "Late Penalties", decimalFormat.format(netwage.getLateDeduction())));
        sb.append(String.format("%-20s: PHP %s%n", "Total Deductions", decimalFormat.format(netwage.getTotalDeductions())));
        sb.append(String.format("%-20s: PHP %s%n", "Withholding Tax", decimalFormat.format(netwage.getWithholdingTax())));

        sb.append(String.format("-----------------------%n"));
        sb.append(String.format("%-20s: PHP %s%n", "NET WAGE",
                decimalFormat.format(grosswage.calculate() - netwage.getTotalDeductions() - netwage.getWithholdingTax())));

        return sb.toString();
    }

    // ================== UTILITY METHODS ================== //
    private static Employee findEmployeeById(String empId) {
        List<Employee> employees = EmployeeModelFromFile.getEmployeeModelList();
        for (Employee employee : employees) {
            if (employee.getEmployeeNumber().equals(empId)) {
                return employee;
            }
        }
        return null;
    }

    private static void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(mainFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
