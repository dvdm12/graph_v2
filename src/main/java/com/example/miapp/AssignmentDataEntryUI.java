/*package com.example.miapp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class AssignmentDataEntryUI extends JFrame {
    private JTable assignmentTable;
    private DefaultTableModel tableModel;
    private List<Assignment> assignments;
    private ConflictGraphLoader graphLoader;

    public AssignmentDataEntryUI() {
        setTitle("Assignment Data Entry");
        setSize(1400, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        assignments = new ArrayList<>();
        graphLoader = new ConflictGraphLoader();
        setupUI();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new GridLayout(11, 2, 10, 10));

        // ID
        JTextField idField = new JTextField();
        inputPanel.add(new JLabel("ID:"));
        inputPanel.add(idField);

        // Professor ID
        JTextField professorIdField = new JTextField();
        inputPanel.add(new JLabel("Professor ID:"));
        inputPanel.add(professorIdField);

        // Room (Lab 101 - Lab 115)
        String[] rooms = new String[15];
        for (int i = 0; i < 15; i++) {
            rooms[i] = "Lab " + (101 + i);
        }
        JComboBox<String> roomComboBox = new JComboBox<>(rooms);
        inputPanel.add(new JLabel("Room:"));
        inputPanel.add(roomComboBox);

        // Subject (Subject 1 - Subject 20)
        String[] subjects = new String[20];
        for (int i = 0; i < 20; i++) {
            subjects[i] = "Subject " + (i + 1);
        }
        JComboBox<String> subjectComboBox = new JComboBox<>(subjects);
        inputPanel.add(new JLabel("Subject Group ID:"));
        inputPanel.add(subjectComboBox);

        // Session Period
        JComboBox<String> sessionPeriodComboBox = new JComboBox<>(new String[]{"D", "N"});
        inputPanel.add(new JLabel("Session Period (D/N):"));
        inputPanel.add(sessionPeriodComboBox);

        // Day
        JComboBox<String> dayComboBox = new JComboBox<>(new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"});
        inputPanel.add(new JLabel("Day:"));
        inputPanel.add(dayComboBox);

        // Start Time
        JComboBox<String> startTimeComboBox = new JComboBox<>(generateTimeSlots());
        inputPanel.add(new JLabel("Start Time:"));
        inputPanel.add(startTimeComboBox);

        // End Time
        JComboBox<String> endTimeComboBox = new JComboBox<>(generateTimeSlots());
        inputPanel.add(new JLabel("End Time:"));
        inputPanel.add(endTimeComboBox);

        // Requires Lab
        JCheckBox requiresLabCheck = new JCheckBox();
        inputPanel.add(new JLabel("Requires Lab:"));
        inputPanel.add(requiresLabCheck);

        // Enrolled Students
        JTextField enrolledStudentsField = new JTextField();
        inputPanel.add(new JLabel("Enrolled Students:"));
        inputPanel.add(enrolledStudentsField);

        // Add Buttons
        JButton addButton = new JButton("Add Assignment");
        JButton saveButton = new JButton("Save to JSON");
        inputPanel.add(addButton);
        inputPanel.add(saveButton);

        mainPanel.add(inputPanel, BorderLayout.WEST);

        // Table for Assignments
        String[] columns = {"ID", "Professor ID", "Room", "Subject Group", "Session Period", "Day", "Start Time", "End Time", "Requires Lab", "Enrolled Students"};
        tableModel = new DefaultTableModel(columns, 0);
        assignmentTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(assignmentTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        addButton.addActionListener(e -> addAssignment(idField, professorIdField, roomComboBox, subjectComboBox, sessionPeriodComboBox, dayComboBox, startTimeComboBox, endTimeComboBox, requiresLabCheck, enrolledStudentsField));
        saveButton.addActionListener(e -> saveToJson());
    }

    private String[] generateTimeSlots() {
        List<String> timeSlots = new ArrayList<>();
        for (int hour = 7; hour <= 22; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                timeSlots.add(String.format("%02d:%02d", hour, minute));
            }
        }
        return timeSlots.toArray(new String[0]);
    }

    private void addAssignment(JTextField idField, JTextField professorIdField, JComboBox<String> roomComboBox, JComboBox<String> subjectComboBox, JComboBox<String> sessionPeriodComboBox, JComboBox<String> dayComboBox, JComboBox<String> startTimeComboBox, JComboBox<String> endTimeComboBox, JCheckBox requiresLabCheck, JTextField enrolledStudentsField) {
        try {
            int id = Integer.parseInt(idField.getText());
            int professorId = Integer.parseInt(professorIdField.getText());
            int roomId = Integer.parseInt(((String) roomComboBox.getSelectedItem()).replace("Lab ", ""));
            int subjectGroupId = Integer.parseInt(((String) subjectComboBox.getSelectedItem()).replace("Subject ", ""));
            String sessionPeriod = (String) sessionPeriodComboBox.getSelectedItem();
            String day = (String) dayComboBox.getSelectedItem();
            LocalTime start = LocalTime.parse((String) startTimeComboBox.getSelectedItem());
            LocalTime end = LocalTime.parse((String) endTimeComboBox.getSelectedItem());
            boolean requiresLab = requiresLabCheck.isSelected();
            int enrolledStudents = Integer.parseInt(enrolledStudentsField.getText());

            if (end.isBefore(start) || end.equals(start)) {
                JOptionPane.showMessageDialog(this, "End time must be after start time.", "Invalid Time", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Assignment assignment = new Assignment(id, professorId, roomId, subjectGroupId, sessionPeriod, day, start, end, requiresLab, enrolledStudents);
            assignments.add(assignment);
            graphLoader.addAssignment(assignment);

            Object[] rowData = {id, professorId, roomId, subjectGroupId, sessionPeriod, day, start, end, requiresLab, enrolledStudents};
            tableModel.addRow(rowData);

            idField.setText("");
            professorIdField.setText("");
            enrolledStudentsField.setText("");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please check your data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AssignmentDataEntryUI ui = new AssignmentDataEntryUI();
            ui.setVisible(true);
        });
    }
}
*/