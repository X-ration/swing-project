package com.adam.swing_project.timer.frontend;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.util.DateTimeUtil;
import com.adam.swing_project.timer.stat.TimerDayStatistic;
import com.adam.swing_project.timer.stat.TimerStatistic;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.regex.Pattern;

public class StatisticDialog extends JDialog {

    public StatisticDialog(JFrame jFrame) {
        super(jFrame, "统计数据");
        setModal(true);

        Date[] dates = TimerStatistic.getInstance().availableDates();
        String[][] tableData = new String[dates.length][4];
        String[] tableHeader = new String[]{"日期","计划计时","实际计时","手动修正"};
        Vector<String> dateVector = new Vector<>();
        dateVector.add("全部");
        for(int i=0;i<dates.length;i++) {
            Date date = dates[i];
            String dateString = DateTimeUtil.wrapDateYearToDay(date);
            tableData[i][0] = dateString;
            dateVector.add(dateString);
            TimerDayStatistic dayStatistic = TimerStatistic.getInstance().getDayStatistic(date);
            tableData[i][1] = DateTimeUtil.wrapTimeHourToSecond(dayStatistic.getTotalResetTime());
            tableData[i][2] = DateTimeUtil.wrapTimeHourToSecond(dayStatistic.getTotalCountedTime());
            tableData[i][3] = "修正";
        }
        StatisticTableModel tableModel = new StatisticTableModel(tableData, tableHeader);

        JTable jTable = new JTable(tableModel);
        jTable.setDefaultRenderer(String.class, new StatisticDefaultTableCellRenderer());
        jTable.getTableHeader().setFont(jTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        jTable.getColumnModel().getColumn(3).setCellRenderer(new StatisticButtonTableCellRenderer());
        jTable.getColumnModel().getColumn(3).setCellEditor(new StatisticButtonTableCellEditor());
        jTable.getColumnModel().getColumn(1).setCellEditor(new StatisticTimeTableCellEditor());
        jTable.getColumnModel().getColumn(2).setCellEditor(new StatisticTimeTableCellEditor());
        JScrollPane jScrollPane = new JScrollPane(jTable);
        Dimension tablePreferredSize = jTable.getPreferredSize(),
                viewPortPreferredSize = jScrollPane.getViewport().getPreferredSize();
        if(tablePreferredSize.height < viewPortPreferredSize.height) {
            jScrollPane.getViewport().setPreferredSize(new Dimension(viewPortPreferredSize.width, tablePreferredSize.height));
        }
        jScrollPane.getViewport().setMinimumSize(jScrollPane.getViewport().getPreferredSize());
        JComboBox<String> dateComboBox = new JComboBox<>(dateVector);
        dateComboBox.addActionListener(e -> {
            int selectedIndex = dateComboBox.getSelectedIndex();
            if(selectedIndex == 0) {
                tableModel.filterDate(null);
            } else {
                tableModel.filterDate(tableData[selectedIndex-1][0]);
            }
        });
        JLabel dateLabel = new JLabel("选择日期：");
        dateLabel.setHorizontalAlignment(JLabel.RIGHT);
        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> dispose());

        JPanel datePanel = new JPanel();
        datePanel.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints;
        gridBagConstraints = new GridBagConstraints(0,0,1,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        datePanel.add(dateLabel, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(1,0,1,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        datePanel.add(dateComboBox, gridBagConstraints);

        setLayout(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints(0,0,2,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        add(datePanel, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,1,2,1,1,1,
                GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0);
        add(jScrollPane, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,2,2,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        add(okButton, gridBagConstraints);

        pack();
        setLocationRelativeTo(jFrame);
        setMinimumSize(getPreferredSize());
    }

    private class StatisticTimeTableCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField jTextField = new JTextField();
        private final Pattern TIME_PATTERN = Pattern.compile("\\d{2}:[0-5]\\d:[0-5]\\d");
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            jTextField.setText((String) value);
            jTextField.setHorizontalAlignment(JTextField.CENTER);
            jTextField.setFont(jTextField.getFont().deriveFont(Font.BOLD));
            jTextField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            return jTextField;
        }

        @Override
        public Object getCellEditorValue() {
            String text = jTextField.getText();
            return text;
        }

        @Override
        public boolean stopCellEditing() {
            String editorValue = (String) getCellEditorValue();
            if(!editorValue.equals("") && !TIME_PATTERN.matcher(editorValue).matches()) {
                JOptionPane.showMessageDialog(jTextField, "请检查输入格式！", "错误的输入", JOptionPane.WARNING_MESSAGE);
                jTextField.select(0, jTextField.getDocument().getLength());
                return false;
            }
            return super.stopCellEditing();
        }

        @Override
        public void cancelCellEditing() {
            jTextField.setText("");
            super.cancelCellEditing();
        }
    }

    private class StatisticButtonTableCellEditor extends AbstractCellEditor implements TableCellEditor{
        private class ButtonActionListener implements ActionListener {
            int mode = 1;
            final JTable table;
            final JButton jButton;
            final int row;
            ButtonActionListener(JTable table, JButton jButton, int row) {
                this.table = table;
                this.jButton = jButton;
                this.row = row;
            }

            void setMode1() {
                StatisticTableModel model = (StatisticTableModel) table.getModel();
                table.getCellEditor(row, 1).cancelCellEditing();
                table.getCellEditor(row, 2).cancelCellEditing();
                model.setRowEditStop(row);
                jButton.setText("修正");
                this.mode = 1;
            }

            void reset() {
                table.getCellEditor(row, 1).cancelCellEditing();
                table.getCellEditor(row, 2).cancelCellEditing();
                jButton.setText("修正");
                this.mode = 1;
            }

            void setMode2() {
                StatisticTableModel model = (StatisticTableModel) table.getModel();
                model.setRowEditStart(row);
                jButton.setText("完成修正");
                this.mode = 2;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == 1) {
                    setMode2();
                } else {
                    setMode1();
                }
            }
        }
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            JButton jButton = new JButton((String) value);
            jButton.setFont(jButton.getFont().deriveFont(Font.PLAIN));
            String dateString = (String) table.getModel().getValueAt(row, 0);
            if(DateTimeUtil.getCurrentDate().equals(DateTimeUtil.unwrapDateYearToDay(dateString))) {
                jButton.setEnabled(false);
            } else {
                ButtonActionListener actionListener = new ButtonActionListener(table, jButton, row);
                jButton.addActionListener(actionListener);
                table.getModel().addTableModelListener(e -> {
                    if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE
                            && e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                        actionListener.reset();
                    }
                });
            }
            return jButton;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }

        @Override
        public boolean stopCellEditing() {
            return true;
        }
    }

    private class StatisticButtonTableCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JButton jButton = new JButton((String) value);
            jButton.setFont(jButton.getFont().deriveFont(Font.PLAIN));
            String dateString = (String) table.getModel().getValueAt(row, 0);
//            if(DateTimeUtil.getCurrentDate().equals(DateTimeUtil.unwrapDateYearToDay(dateString))) {
//                jButton.setEnabled(false);
//            }
            return jButton;
        }
    }

    private class StatisticDefaultTableCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel jLabel = new JLabel((String)value);
            jLabel.setHorizontalAlignment(JLabel.CENTER);
            jLabel.setFont(jLabel.getFont().deriveFont(Font.PLAIN));
            return jLabel;
        }
    }

    private class StatisticTableModel extends AbstractTableModel {
        final String[][] data;
        final String[] header;
        int editingRow;
        boolean rowEditing;
        FilteredRowView view;

        private class FilteredRowView extends AbstractTableModel{
            int actualRow = -1;
            FilteredRowView(int actualRow) {
                if(actualRow >= 0 && actualRow < StatisticTableModel.this.data.length) {
                    this.actualRow = actualRow;
                }
            }

            @Override
            public int getRowCount() {
                return actualRow == -1 ? 0 : 1;
            }

            @Override
            public int getColumnCount() {
                return StatisticTableModel.this.getColumnCount();
            }

            @Override
            public String getValueAt(int rowIndex, int columnIndex) {
                if(actualRow != -1 && rowIndex == 0) {
                    return StatisticTableModel.this.data[actualRow][columnIndex];
                }
                return null;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if(actualRow != -1 && rowIndex == 0) {
                    StatisticTableModel.this.data[actualRow][columnIndex] = (String) aValue;
                }
            }
        }

        StatisticTableModel(String[][] data, String[] header) {
            this.data = data;
            this.header = header;
        }

        @Override
        public int getRowCount() {
            if(view != null) {
                return view.getRowCount();
            }
            return data.length;
        }

        @Override
        public int getColumnCount() {
            if(view != null) {
                return view.getColumnCount();
            }
            return header.length;
        }

        @Override
        public String getValueAt(int rowIndex, int columnIndex) {
            if(view != null) {
                return view.getValueAt(rowIndex, columnIndex);
            }
            return data[rowIndex][columnIndex];
        }

        @Override
        public String getColumnName(int column) {
            return header[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        void setRowEditStart(int row) {
            rowEditing = true;
            editingRow = row;
        }

        void setRowEditStop(int row) {
            rowEditing = false;
            editingRow = -1;
            String dateString = getValueAt(row, 0);
            String totalResetTimeString = getValueAt(row, 1);
            String totalCountedTimeString = getValueAt(row, 2);
            Date date = DateTimeUtil.unwrapDateYearToDay(dateString);
            Time totalResetTime = DateTimeUtil.unwrapTimeHourToSecond(totalResetTimeString),
                    totalCountedTime = DateTimeUtil.unwrapTimeHourToSecond(totalCountedTimeString);
            TimerStatistic.getInstance().reviseDayStatistic(date, totalResetTime, totalCountedTime);
        }

        void filterDate(String dateString) {
            if(dateString != null) {
                int row = -1;
                for (int i = 0; i < data.length; i++) {
                    if (data[i][0].equals(dateString)) {
                        row = i;
                    }
                }
                view = new FilteredRowView(row);
            } else {
                view = null;
            }
            rowEditing = false;
            editingRow = -1;
            fireTableDataChanged();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if(view != null) {
                view.setValueAt(aValue, rowIndex, columnIndex);
                return;
            }
            data[rowIndex][columnIndex] = (String) aValue;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 3)
                return true;
            if(rowEditing && rowIndex == editingRow && (columnIndex == 1 || columnIndex == 2))
                return true;
            return false;
        }
    }

}
