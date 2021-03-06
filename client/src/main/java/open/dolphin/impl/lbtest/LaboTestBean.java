package open.dolphin.impl.lbtest;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.AbstractChartDocument;
import open.dolphin.client.ClientContext;
import open.dolphin.client.GUIFactory;
import open.dolphin.client.NameValuePair;
import open.dolphin.delegater.LaboDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.Layer;

/**
 * LaboTestBean
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 *
 */
public class LaboTestBean extends AbstractChartDocument {

    private static final String TITLE = "ラボテスト";
    private static final int DEFAULT_DIVIDER_LOC = 210;
    private static final int DEFAULT_DIVIDER_WIDTH = 10;
    private static final String COLUMN_HEADER_ITEM = "項 目";
    //private static final String GRAPH_TITLE = "検査結果";
    //private static final String X_AXIS_LABEL = "検体採取日";
    //private static final String GRAPH_TITLE_LINUX = "Lab. Test";
    //private static final String X_AXIS_LABEL_LINUX = "Sampled Date";
    private static final int FONT_SIZE_WIN = 12;
    private static final String FONT_MS_GOTHIC = "MSGothic";
    //private static final int MAX_RESULT = 5;
    //private static final String[] EXTRACTION_MENU = new String[]{"5回分", "0", "6~10回分", "5"};
    private static final int MAX_RESULT = 6;
    // 全件表示修正^
    //private static final String[] EXTRACTION_MENU = new String[]{"6回分", "0", "7~12回分", "6"};

    private ListTableModel<LabTestRowObject> tableModel;
    private JTable table;
    private JPanel graphPanel;

    private JComboBox extractionCombo;
    private JTextField countField;
    private LaboDelegater ldl;
    private int dividerWidth;
    private int dividerLoc;

    // １回の検索で得る抽出件数
    private int maxResult = MAX_RESULT;

//minagawa^ 全件表示修正^
    // 抽出メニュー
    //private String[] extractionMenu = EXTRACTION_MENU;
//minagawa$
    private boolean widthAdjusted;
    
//s.oh^ ラボテストの印刷
    //private JButton printBtn;
//s.oh$
//s.oh^ ラボテストのPDF出力
    private JFreeChart chart;
    private JButton pdfBtn;
    private JButton printPdfBtn;    // ラボデータPDFの印刷
//s.oh$
    
    // ラボデータの削除 2013/06/24
    private List<NLaboModule> modules;
    
    public LaboTestBean() {
        setTitle(TITLE);
    }

    public int getMaxResult() {
        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

//minagawa^ 全件表示修正^
//    public String[] getExtractionMenu() {
//        return extractionMenu;
//    }
//
//    public void setExtractionMenu(String[] extractionMenu) {
//        this.extractionMenu = extractionMenu;
//    }
//minagawa$
    public void createTable(List<NLaboModule> moduleList) {

        // 現在のデータをクリアする
        if (tableModel != null && tableModel.getDataProvider() != null) {
            tableModel.getDataProvider().clear();
        }

        // グラフもクリアする
        graphPanel.removeAll();
        graphPanel.validate();

        // Table のカラムヘッダーを生成する
        String[] header = new String[getMaxResult() + 1];
        header[0] = COLUMN_HEADER_ITEM;
        for (int col = 1; col < header.length; col++) {
            header[col] = "";
        }
//minagawa^ LSC 1.4 bug fix ラボデータの削除 2013/06/24
        if (modules!=null) {
            modules.clear();
        }
        modules = moduleList;
//minagawa$        

        // 結果がゼロであれば返る
        if (modules == null || modules.isEmpty()) {
            tableModel = new ListTableModel<LabTestRowObject>(header, 0);
            table.setModel(tableModel);
            setColumnWidth();
            //printBtn.setEnabled(false);
            return;
        }

        // 検体採取日の降順なので昇順にソートする
//s.oh^ 2013/06/13 カラムの並び順
        //Collections.sort(modules, new SampleDateComparator());
        if(!Project.getBoolean("labtest.column.newest.left", false)) {
            Collections.sort(modules, new SampleDateComparator());
        }
//s.oh$

        // テスト項目全てに対応する rowObject を生成する
        List<LabTestRowObject> dataProvider = new ArrayList<LabTestRowObject>();

        int moduleIndex = 0;

        for (NLaboModule module : modules) {

            // 検体採取日
            header[moduleIndex+1] = module.getSampleDate();

            // モジュールに含まれる検査項目
            Collection<NLaboItem> c = module.getItems();

            for (NLaboItem item : c) {

                // RowObject を生成し dataProvider へ加える
                // 最初のモジュールのテスト項目は無条件に加える
                if (moduleIndex == 0) {
                    // row
                    LabTestRowObject row = new LabTestRowObject();
                    row.setLabCode(item.getLaboCode());
                    row.setGroupCode(item.getGroupCode());
                    row.setParentCode(item.getParentCode());
                    row.setItemCode(item.getItemCode());
                    row.setItemName(item.getItemName());
                    row.setUnit(item.getUnit());
                    row.setNormalValue(item.getNormalValue());
                    // valueを moduleIndex番目にセットする
                    LabTestValueObject value = new LabTestValueObject();
                    value.setSampleDate(module.getSampleDate());
                    value.setValue(item.getValue());
                    value.setOut(item.getAbnormalFlg());
                    value.setComment1(item.getComment1());
                    value.setComment2(item.getComment2());
                    row.addLabTestValueObjectAt(moduleIndex, value);
                    //
                    dataProvider.add(row);
                    continue;
                }

                // 二つ目のモジュールからは無かったら加える
                boolean found = false;

                for (LabTestRowObject rowObject : dataProvider) {
                    if (item.getItemCode().equals(rowObject.getItemCode())) {
                        found = true;
                        LabTestValueObject value = new LabTestValueObject();
                        value.setSampleDate(module.getSampleDate());
                        value.setValue(item.getValue());
                        value.setOut(item.getAbnormalFlg());
                        value.setComment1(item.getComment1());
                        value.setComment2(item.getComment2());
                        rowObject.addLabTestValueObjectAt(moduleIndex, value);
                        break;
                    }
                }

                if (!found) {
                    LabTestRowObject row = new LabTestRowObject();
                    row.setLabCode(item.getLaboCode());
                    row.setGroupCode(item.getGroupCode());
                    row.setParentCode(item.getParentCode());
                    row.setItemCode(item.getItemCode());
                    row.setItemName(item.getItemName());
                    row.setUnit(item.getUnit());
                    row.setNormalValue(item.getNormalValue());
                    //
                    LabTestValueObject value = new LabTestValueObject();
                    value.setSampleDate(module.getSampleDate());
                    value.setValue(item.getValue());
                    value.setOut(item.getAbnormalFlg());
                    value.setComment1(item.getComment1());
                    value.setComment2(item.getComment2());
                    row.addLabTestValueObjectAt(moduleIndex, value);
                    //
                    dataProvider.add(row);
                }
            }

            moduleIndex++;
        }

        // dataProvider の要素 rowObject をソートする
        // grpuCode,parentCode,itemcode;
        Collections.sort(dataProvider);

        // Table Model
        tableModel = new ListTableModel<LabTestRowObject>(header, 0);

        // 検査結果テーブルを生成する
        table.setModel(tableModel);
        setColumnWidth();

        // dataProvider
        tableModel.setDataProvider(dataProvider);
        
        //printBtn.setEnabled(true);
//minagawa^ LSC 1.4 bug fix ラボデータの削除 2013/06/24
        table.getTableHeader().addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
            
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = table.getTableHeader().columnAtPoint(e.getPoint());
                    if (index==0) {
                        return;
                    }
                    final NLaboModule toDelete = modules.get(index-1);
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(new AbstractAction("削 除") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            String date = toDelete.getSampleDate().replaceAll(" 00:00", "");
                            StringBuilder sb = new StringBuilder();
                            sb.append(date).append(" の検査を削除しますか？").append("\n");
                            sb.append("この操作は取り消せません。");
                            String msg = sb.toString();
                            String[] options = {GUIFactory.getCancelButtonText(),"削 除"};
                            int val = JOptionPane.showOptionDialog(
                                getUI(), msg, ClientContext.getFrameTitle("検体検査削除"),
                                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                            switch(val) {
                                case 0:
                                    break;
                                case 1:
                                    deleteLabTest(toDelete.getId());
                                    break;
                            }
                        }
                    });
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
//minagawa$        
    }

    /**
     * Tableのカラム幅を調整する。
     */
    private void setColumnWidth() {
//s.oh^ 2013/03/19 機能改善(カラムを同一横幅)
        //// カラム幅を調整する
        //if (!widthAdjusted) {
        //    table.getTableHeader().getColumnModel().getColumn(0).setPreferredWidth(180);
        //    table.getTableHeader().getColumnModel().getColumn(1).setPreferredWidth(100);
        //    table.getTableHeader().getColumnModel().getColumn(2).setPreferredWidth(100);
        //    table.getTableHeader().getColumnModel().getColumn(3).setPreferredWidth(100);
        //    table.getTableHeader().getColumnModel().getColumn(4).setPreferredWidth(100);
        //    table.getTableHeader().getColumnModel().getColumn(5).setPreferredWidth(100);
        //    widthAdjusted = true;
        //}
    }

    /**
     * GUIコンポーネントを初期化する。
     */
    private void initialize() {

        // Divider
        dividerWidth = DEFAULT_DIVIDER_WIDTH;
        dividerLoc = DEFAULT_DIVIDER_LOC;

        JPanel controlPanel = createControlPanel();

        graphPanel = new JPanel(new BorderLayout());
        graphPanel.setPreferredSize(new Dimension(500, dividerLoc));

        // 検査結果テーブルを生成する
        table = new JTable();

        // Rendererを設定する
        //table.setDefaultRenderer(Object.class, new LabTestRenderer());
        LabTestRenderer rederer = new LabTestRenderer();
        rederer.setTable(table);
        rederer.setDefaultRenderer();
        
        // 行高
        table.setRowHeight(ClientContext.getHigherRowHeight());

        // 行選択を可能にする
        table.setRowSelectionAllowed(true);
        
        table.getTableHeader().setReorderingAllowed(false);

        //-----------------------------------------------
        // Copy 機能を実装する
        //-----------------------------------------------
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        final AbstractAction copyAction = new AbstractAction("コピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyRow();
            }
        };
        
        final AbstractAction copyLatestAction = new AbstractAction("直近の結果のみコピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyLatest();
            }
        };

        table.getInputMap().put(copy, "Copy");
        table.getActionMap().put("Copy", copyLatestAction);

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent me) {
                mabeShowPopup(me);
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                mabeShowPopup(me);
            }

            public void mabeShowPopup(MouseEvent e) {

                if (!e.isPopupTrigger()) {
                    return;
                }

                int row = table.rowAtPoint(e.getPoint());

                if (row < 0 ) {
                    return;
                }
                
//s.oh^ 2013/10/08 ラボテスト
                int[] selecteds = table.getSelectedRows();
                if(selecteds == null || selecteds.length <= 0) {
                    return;
                }
//s.oh$

                JPopupMenu contextMenu = new JPopupMenu();
                contextMenu.add(new JMenuItem(copyLatestAction));
                contextMenu.add(new JMenuItem(copyAction));

                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // グラフ表示のリスナを登録する
        ListSelectionModel m = table.getSelectionModel();
        m.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    createAndShowGraph(table.getSelectedRows());
                }
            }
        });

        JScrollPane jScrollPane1 = new JScrollPane();
        jScrollPane1.setViewportView(table);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(3, 600));

        JPanel tablePanel = new JPanel(new BorderLayout(0, 7));
        tablePanel.add(controlPanel, BorderLayout.SOUTH);
        tablePanel.add(jScrollPane1, BorderLayout.CENTER);

        // Lyouts
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphPanel, tablePanel);
        splitPane.setDividerSize(dividerWidth);
        splitPane.setContinuousLayout(false);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(dividerLoc);

        getUI().setLayout(new BorderLayout());
        getUI().add(splitPane, BorderLayout.CENTER);

        getUI().setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
        
//s.oh^ 不具合修正
        enter();
//s.oh$
    }

    @Override
    public void start() {
        initialize();
//minagawa^ LSC Test        
        enter();
//minagawa$        
// 全件表示修正^
//        NameValuePair pair = (NameValuePair) extractionCombo.getSelectedItem();
//        String value = pair.getValue();
//        int firstResult = Integer.parseInt(value);
//        searchLaboTest(firstResult);
        firstSearch();
    }

    @Override
    public void stop() {
        if (tableModel != null && tableModel.getDataProvider() != null) {
            tableModel.getDataProvider().clear();
        }
    }

    /**
     * 選択されている行で直近のデータをコピーする。
     */
    public void copyLatest() {
        StringBuilder sb = new StringBuilder();
        int numRows=table.getSelectedRowCount();
        int[] rowsSelected=table.getSelectedRows();
        for (int i = 0; i < numRows; i++) {
            LabTestRowObject rdm = tableModel.getObject(rowsSelected[i]);
            if (rdm != null) {
//s.oh^ 2013/06/13 カラムの並び順
                //sb.append(rdm.toClipboardLatest()).append("\n");
                if(!Project.getBoolean("labtest.column.newest.left", false)) {
                    sb.append(rdm.toClipboardLatest()).append("\n");
                }else{
                    sb.append(rdm.toClipboardLatestReverse()).append("\n");
                }
//s.oh$
            }
        }
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
        }
    }

    /**
     * 選択されている行をコピーする。
     */
    public void copyRow() {
        StringBuilder sb = new StringBuilder();
        int numRows=table.getSelectedRowCount();
        int[] rowsSelected=table.getSelectedRows();
        for (int i = 0; i < numRows; i++) {
            LabTestRowObject rdm = tableModel.getObject(rowsSelected[i]);
            if (rdm != null) {
                sb.append(rdm.toClipboard()).append("\n");
            }
        }
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
        }
    }

    /**
     * LaboTest の検索タスクをコールする。
     */
    private void searchLaboTest(final int firstResult) {

        final String pid = getContext().getPatient().getPatientId();
        ldl = new LaboDelegater();

        DBTask task = new DBTask<List<NLaboModule>, Void>(getContext()) {

            @Override
            protected List<NLaboModule> doInBackground() throws Exception {
                List<NLaboModule> result = ldl.getLaboTest(pid, firstResult, getMaxResult());
                return result;
            }

            @Override
            protected void succeeded(List<NLaboModule> result) {
                int moduleCount = result != null ? result.size() : 0;
                // 全件表示修正^
                //countField.setText(String.valueOf(moduleCount));
                createTable(result);
            }
        };

        task.execute();

    }
    
    // 全件表示修正^
    private void firstSearch() {

//s.oh^ 2013/09/18 ラボデータの高速化
//        final String pid = getContext().getPatient().getPatientId();
//        ldl = new LaboDelegater();
//
//        DBTask task = new DBTask<List<NLaboModule>, Void>(getContext()) {
//
//            @Override
//            protected List<NLaboModule> doInBackground() throws Exception {
//                // 全件取得する maxResult=1000
//                List<NLaboModule> modules = ldl.getLaboTest(pid, 0, 1000);
//                return modules;
//            }
//
//            @Override
//            protected void succeeded(List<NLaboModule> modules) {
//                
//                // 全件数
//                int moduleCount = modules != null ? modules.size() : 0;
//                
//                // ComboBox へ表示するItemの数
//                int itemCount = moduleCount/getMaxResult();
//                if (moduleCount%getMaxResult()!=0) {
//                    itemCount+=1;
//                }
//                
//                // Loopしてcomboboxへ加える
//                for (int i=0; i < itemCount; i++) {
//                    int firstIndex = i*getMaxResult();
//                    int lastIndex = firstIndex+getMaxResult()-1;
//                    if (lastIndex>(moduleCount-1)) {
//                        lastIndex = moduleCount-1;
//                    }
//                    StringBuilder sb = new StringBuilder();
//                    if (i!=0) {
//                        sb.append(String.valueOf(firstIndex+1));
//                        sb.append("~");
//                    }
//                    sb.append(String.valueOf(lastIndex+1));
//                    sb.append("回分");
//                    String name = sb.toString();
//                    String value = String.valueOf(firstIndex);
//                    NameValuePair item = new NameValuePair(name, value);
//                    extractionCombo.addItem(item);
//                }
//                countField.setText(String.valueOf(moduleCount));
//                
//                // 最初の６回分を表示させる
//                int cnt = moduleCount>=getMaxResult() ? getMaxResult() : moduleCount;
//                List<NLaboModule> list = new ArrayList(cnt);
//                for (int i=0; i <cnt; i++) {
//                    list.add(modules.get(i));
//                }
//                createTable(list);
//            }
//        };
//
//        task.execute();
        // 全件数
        String pid = getContext().getPatient().getPatientId();
        ldl = new LaboDelegater();
        int moduleCount = Integer.parseInt(ldl.getLaboTestCount(pid));

        // ComboBox へ表示するItemの数
        int itemCount = moduleCount/getMaxResult();
        if (moduleCount%getMaxResult()!=0) {
            itemCount+=1;
        }

        // Loopしてcomboboxへ加える
        for (int i=0; i < itemCount; i++) {
            int firstIndex = i*getMaxResult();
            int lastIndex = firstIndex+getMaxResult()-1;
            if (lastIndex>(moduleCount-1)) {
                lastIndex = moduleCount-1;
            }
            StringBuilder sb = new StringBuilder();
            if (i!=0) {
                sb.append(String.valueOf(firstIndex+1));
                sb.append("~");
            }
            sb.append(String.valueOf(lastIndex+1));
            sb.append("回分");
            String name = sb.toString();
            String value = String.valueOf(firstIndex);
            NameValuePair item = new NameValuePair(name, value);
            extractionCombo.addItem(item);
        }
        countField.setText(String.valueOf(moduleCount));
//s.oh$
    }
    
//minagawa^ LSC 1.4 bug fix ラボデータの削除 2013/06/24
    private void deleteLabTest(final long moduleId) {

        final String pid = getContext().getPatient().getPatientId();
        ldl = new LaboDelegater();

        DBTask task = new DBTask<Integer, Void>(getContext()) {

            @Override
            protected Integer doInBackground() throws Exception {
                int result = ldl.deleteLabTest(moduleId);
                return new Integer(result);
            }

            @Override
            protected void succeeded(Integer result) {
                if (extractionCombo.getSelectedIndex()==0) {
                    searchLaboTest(0);
                } else {
                    extractionCombo.setSelectedIndex(0);
                }
            }
        };

        task.execute();
    }
//minagawa$    

    /**
     * 検査結果テーブルで選択された行（検査項目）の折れ線グラフを生成する。
     * 複数選択対応
     * JFreeChart を使用する。
     */
    private void createAndShowGraph(int[] selectedRows) {

        if (selectedRows == null || selectedRows.length == 0) {
            return;
        }
        
//s.oh^ ラボテストのPDF出力
        // 日本語が文字化けしないテーマ(必要な場合は有効にする)
        //ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
//s.oh$
        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 選択されている行（検査項目）をイテレートし、dataset へ値を設定する
        for (int cnt = 0; cnt < selectedRows.length; cnt++) {

            int row = selectedRows[cnt];
            List<LabTestRowObject> dataProvider = tableModel.getDataProvider();
            LabTestRowObject rowObj = dataProvider.get(row);
            List<LabTestValueObject> values = rowObj.getValues();

            //boolean valueIsNumber = true;
            
            // 検体採取日ごとの値を設定する
            // カラムの１番目から採取日がセットされている
            for (int col = 1; col <= getMaxResult(); col++) {

                String sampleTime = tableModel.getColumnName(col);

                // 検体採取日="" -> 検査なし
                if (sampleTime.equals("")) {
                    break;
                }

                LabTestValueObject value = values.get(col -1);
//masuda^   中止された時などvalueがnullのときがある。そんなときはnull値にする。
                try {
                    double val = Double.parseDouble(value.getValue());
                    dataset.setValue(val, rowObj.nameWithUnit(), sampleTime);
                } catch (Exception e) {
                    dataset.setValue(null, rowObj.nameWithUnit(), sampleTime);
                }
            }
//masuda$
        }

//s.oh^ ラボテストのPDF出力
        //JFreeChart chart = ChartFactory.createLineChart(
        chart = ChartFactory.createLineChart(
//s.oh$
//masuda^                
                    //getGraphTitle(),                // Title
                    //getXLabel(),                    // x-axis Label
                    null, null,
//masuda$                    
                    "",                             // y-axis Label
                    dataset,                        // Dataset
                    PlotOrientation.VERTICAL,       // Plot Orientation
                    true,                           // Show Legend
                    true,                           // Use tooltips
                    false                           // Configure chart to generate URLs?
                    );

        // Win の文字化け
        if (ClientContext.isWin()) {
//masuda^            
            //chart.getTitle().setFont(getWinFont());
//masuda$            
            chart.getLegend().setItemFont(getWinFont());
            chart.getCategoryPlot().getDomainAxis().setLabelFont(getWinFont());
            chart.getCategoryPlot().getDomainAxis().setTickLabelFont(getWinFont());
        }
//masuda^
        // 背景色を設定 薄くする
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(220, 220, 220));
        // グラフにドットをつける
        IgnoreNullLineRenderer renderer = new IgnoreNullLineRenderer();
        plot.setRenderer(renderer);
        // 選択した項目が一つならば基準範囲を表示する
        if (selectedRows.length == 1) {
            List<LabTestRowObject> dataProvider = tableModel.getDataProvider();
            LabTestRowObject rowObj = dataProvider.get(selectedRows[0]);
            String normalValue = rowObj.getNormalValue();
            try {
                String[] values = normalValue.split("-");
                float low = Float.valueOf(values[0]);
                float hi = Float.valueOf(values[1]);
                IntervalMarker marker = new IntervalMarker(low, hi);
                marker.setPaint(new Color(200, 230, 200));
                plot.addRangeMarker(marker, Layer.BACKGROUND);
            } catch (Exception e) {
            }
        }
//masuda$

        ChartPanel chartPanel = new ChartPanel(chart);
//minagawa^        
        chartPanel.setPopupMenu(null);
//minagawa$        

        graphPanel.removeAll();
        graphPanel.add(chartPanel, BorderLayout.CENTER);
        graphPanel.validate();
    }

    //====================================================================
//    private String getGraphTitle() {
//        return ClientContext.isLinux() ? GRAPH_TITLE_LINUX : GRAPH_TITLE;
//    }
//
//    private String getXLabel() {
//        return ClientContext.isLinux() ? X_AXIS_LABEL_LINUX : X_AXIS_LABEL;
//    }

    private Font getWinFont() {
        return new Font(FONT_MS_GOTHIC, Font.PLAIN, FONT_SIZE_WIN);
    }
    //====================================================================

    /**
     * 抽出期間パネルを返す
     */
    private JPanel createControlPanel() {

//minagawa^ 全件表示修正^
//        String[] menu = getExtractionMenu();
//        int cnt = menu.length / 2;
//        NameValuePair[] periodObject = new NameValuePair[cnt];
//        int valIndex = 0;
//        for (int i = 0; i < cnt; i++) {
//            periodObject[i] = new NameValuePair(menu[valIndex], menu[valIndex+1]);
//            valIndex += 2;
//        }
//minagawa$
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalStrut(7));

        // 抽出期間コンボボックス
        p.add(new JLabel("過去"));
        p.add(Box.createRigidArea(new Dimension(5, 0)));
        // 全件表示修正^
        //extractionCombo = new JComboBox(periodObject);
        extractionCombo = new JComboBox();
        extractionCombo.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // 全件表示修正^
                    if (extractionCombo.getSelectedItem()!=null) {
                        NameValuePair pair = (NameValuePair)extractionCombo.getSelectedItem();
                        int firstResult = Integer.parseInt(pair.getValue());
                        searchLaboTest(firstResult);
                    }
                }
            }
        });
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        comboPanel.add(extractionCombo);
        
//s.oh^ ラボテストの印刷
        //// comboPanelと同じグループのパネルに追加する
        //comboPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        //printBtn = new JButton("リスト印刷");
        //printBtn.addActionListener(new ActionListener() {
        //    @Override
        //    public void actionPerformed(ActionEvent e) {
        //        LaboTestPrint print = new LaboTestPrint();
        //        if(table != null) {
        //            print.setTable(table);
        //            MessageFormat header = new MessageFormat(getContext().getPatient().getFullName() + " 様 カルテ");
        //            MessageFormat footer = new MessageFormat("ラボテスト: Page - {0}");
        //            String jobName = getContext().getContext().getPageFormat() + " by Dolphin";
        //            //print.printTable(null, 1, getContext().getPatient().getFullName());
        //            print.printTable(getContext().getContext().getPageFormat(), 1, jobName, header, footer);
        //        }
        //    }
        //});
        //comboPanel.add(printBtn);
//s.oh$

        p.add(comboPanel);

        // グル
        p.add(Box.createHorizontalGlue());

        // 件数フィールド
        p.add(new JLabel("件数"));
        p.add(Box.createRigidArea(new Dimension(5, 0)));
        countField = new JTextField(2);
        countField.setEditable(false);
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        countPanel.add(countField);
        p.add(countPanel);
        
//s.oh^ ラボテストのPDF出力
        // countPanelと同じグループのパネルに追加する
        comboPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        pdfBtn = new JButton("PDF出力");
        pdfBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = createPDF(true);
            }
        });
        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        outputPanel.add(pdfBtn);
        p.add(outputPanel);   // ラボデータPDFの印刷
        
        // ラボデータPDFの印刷
        comboPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        printPdfBtn = new JButton("リスト印刷");
        printPdfBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = createPDF(false);
                LaboTestOutputPDF.printPDF(path);
            }
        });
        outputPanel.add(printPdfBtn);
        p.add(outputPanel);
//s.oh$

        // スペース
        p.add(Box.createHorizontalStrut(7));

        return p;
    }

//s.oh^ ラボテストのPDF出力    
    public void updateList() {
                        NameValuePair pair = (NameValuePair)extractionCombo.getSelectedItem();
                        int firstResult = Integer.parseInt(pair.getValue());
                        searchLaboTest(firstResult);
        graphPanel.removeAll();
        graphPanel.validate();
    }
    
    /**
     * PDFの作成
     * @return 
     */
    private String createPDF(boolean showDlg) {
        if(showDlg) {
            StringBuilder sb = new StringBuilder();
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = chooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                String dir = chooser.getSelectedFile().getPath();
                sb.append(dir);
                LaboTestOutputPDF pdf = new LaboTestOutputPDF(getContext().getPatient().getPatientId(), getContext().getPatient().getFullName(), new Date(), sb.toString(), table, chart);
                return pdf.create();
            }else{
                //sb.append(ClientContext.getTempDirectory());
            }
        }else{
            StringBuilder sb = new StringBuilder();
            sb.append(ClientContext.getTempDirectory());
            LaboTestOutputPDF pdf = new LaboTestOutputPDF(getContext().getPatient().getPatientId(), getContext().getPatient().getFullName(), null, sb.toString(), table, chart);
            return pdf.create();
        }
        return null;
    }
//s.oh$
}
