package open.dolphin.letter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import open.dolphin.client.*;
import open.dolphin.delegater.LetterDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.util.AgeCalculater;
import open.dolphin.util.Log;
import org.apache.log4j.Level;

/**
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public class Reply2Impl extends AbstractChartDocument implements Letter {

    protected static final String TITLE = "ご　報　告";
    protected static final String ITEM_VISITED_DATE = "visited";
    protected static final String TEXT_INFORMED_CONTENT = "informedContent";

    private static final String TITLE__PREFIX = "ご報告:";

    protected LetterModule model;
    protected Reply2View view;
    private boolean listenerIsAdded;

    protected LetterStateMgr stateMgr;
    protected boolean DEBUG;
    
    private boolean modify;

    /** Creates a new instance of LetterDocument */
    public Reply2Impl() {
        setTitle(TITLE);
        DEBUG = ClientContext.getBootLogger().getLevel() == Level.DEBUG ? true : false;
    }
    
//minagawa^ LSC Test    
    public LetterModule getModel() {
        return model;
    }
    
    public void setModel(LetterModule m) {
        this.model = m;
    }
    
    public boolean isModify() {
        return modify;
    }
    
    public void setModify(boolean b) {
        modify = b;
    }
//minagawa$
    
    @Override
    public void modelToView(LetterModule m) {

        if (view == null) {
            view = new Reply2View();
        }

        // 日付
//minagawa^ LSC 1.4 bug fix 文書の印刷日付 2013/06/24
        //String dateStr = LetterHelper.getDateAsString(m.getConfirmed());
        String dateStr = LetterHelper.getDateAsString(m.getStarted());
//minagawa$
        LetterHelper.setModelValue(view.getConfirmed(), dateStr);

        // 紹介元（宛先）医療機関名
        LetterHelper.setModelValue(view.getClientHospital(), m.getClientHospital());

        // 紹介元（宛先）診療科
        LetterHelper.setModelValue(view.getClientDept(), m.getClientDept());

        // 紹介元紹介元（宛先）担当医
        LetterHelper.setModelValue(view.getClientDoctor(), m.getClientDoctor());

        // title
        StringBuilder sb = new StringBuilder();
        sb.append("先生　");
        String title = Project.getString("letter.atesaki.title");
        if (title!=null && (!title.equals("無し"))) {
            sb.append(title);
        }
        LetterHelper.setModelValue(view.getAtesakiLbl(), sb.toString());

        // 患者氏名
        LetterHelper.setModelValue(view.getPatientName(), m.getPatientName());

        // 患者生年月日
        String val = LetterHelper.getBirdayWithAge(m.getPatientBirthday(), m.getPatientAge());
        LetterHelper.setModelValue(view.getPatientBirthday(), val);

        // 紹介先（差出人）住所
        if (m.getConsultantAddress()==null) {
            m.setConsultantZipCode(Project.getUserModel().getFacilityModel().getZipCode());
            m.setConsultantAddress(Project.getUserModel().getFacilityModel().getAddress());
        }
        val = LetterHelper.getAddressWithZipCode(m.getConsultantAddress(), m.getConsultantZipCode());
        LetterHelper.setModelValue(view.getConsultantAddress(), val);

        // 紹介先（差出人）電話
        if (m.getConsultantTelephone()==null) {
            m.setConsultantTelephone(Project.getUserModel().getFacilityModel().getTelephone());
        }
        LetterHelper.setModelValue(view.getConsultantTelephone(), m.getConsultantTelephone());

        // 紹介先（差出人）病院名
        LetterHelper.setModelValue(view.getConsultantHospital(), m.getConsultantHospital());

        // 紹介（差出人）先医師
        LetterHelper.setModelValue(view.getConsultantDoctor(), m.getConsultantDoctor());

        //----------------------------------------------------------------------

        // 来院日
        String value = model.getItemValue(ITEM_VISITED_DATE);
        if (value != null) {
            LetterHelper.setModelValue(view.getVisited(), value);
        }

        // Informed
        String text = model.getTextValue(TEXT_INFORMED_CONTENT);
        if (text!=null) {
            LetterHelper.setModelValue(view.getInformedContent(), text);
        }
    }

    @Override
    // 2013/06/24
    //public void viewToModel() {
    public void viewToModel(boolean save) {

//minagawa^ LSC 1.4 bug fix 文書の印刷日付 2013/06/24
//        long savedId = model.getId();
//        model.setId(0L);
//        model.setLinkId(savedId);
//
//        Date d = new Date();
//        model.setConfirmed(d);
//        model.setRecorded(d);
//        model.setKarteBean(getContext().getKarte());
//        model.setUserModel(Project.getUserModel());
//        model.setStatus(IInfoModel.STATUS_FINAL);
        
        if (save) {
            if (model.getId()==0L) {
                // 新規作成で保存 日時を現時刻で再設定する
                Date d = new Date();
                this.model.setConfirmed(d);
                this.model.setRecorded(d);
                this.model.setStarted(d); 
            } else {
                // 修正で保存
                Date d = new Date();
                model.setConfirmed(d);              // 確定日
                model.setRecorded(d);               // 記録日
                model.setLinkId(model.getId());     // LinkId
                model.setId(0L);                    // id=0L -> 常に新規保存 persit される、元のモデルは削除される（要変更）
            }
        }
//minagawa$    

        // 紹介元（宛先）
        model.setClientHospital(LetterHelper.getFieldValue(view.getClientHospital()));
        model.setClientDept(LetterHelper.getFieldValue(view.getClientDept()));
        model.setClientDoctor(LetterHelper.getFieldValue(view.getClientDoctor()));

        // 患者情報、差し出し人側はtartでmodelに設定済

        // 来院日
        String value = LetterHelper.getFieldValue(view.getVisited());
        if (value != null) {
            LetterItem item = new LetterItem(ITEM_VISITED_DATE, value);
            model.addLetterItem(item);
        }

        // Informed
        String informed = LetterHelper.getAreaValue(view.getInformedContent());
        if (informed!=null) {
            LetterText text = new LetterText();
            text.setName(TEXT_INFORMED_CONTENT);
            text.setTextValue(informed);
            model.addLetterText(text);
        }

        // Title
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE__PREFIX).append(model.getClientHospital());
        model.setTitle(sb.toString());
    }

    @Override
    public void start() {

        if (this.model==null) {
            this.model = new LetterModule();

            // Handle Class
            this.model.setHandleClass(Reply2Viewer.class.getName());
            this.model.setLetterType(IInfoModel.CONSULTANT);

            // 確定日等
            Date d = new Date();
            this.model.setConfirmed(d);
            this.model.setRecorded(d);
            this.model.setStarted(d);
            this.model.setStatus(IInfoModel.STATUS_FINAL);
            this.model.setKarteBean(getContext().getKarte());
            this.model.setUserModel(Project.getUserModel());

            // 患者情報
            PatientModel patient = getContext().getPatient();
            this.model.setPatientId(patient.getPatientId());
            this.model.setPatientName(patient.getFullName());
            this.model.setPatientKana(patient.getKanaName());
            this.model.setPatientGender(patient.getGenderDesc());
            this.model.setPatientBirthday(patient.getBirthday());

            int showMonth = Project.getInt(Project.KARTE_AGE_TO_NEED_MONTH);
            String age = AgeCalculater.getAge(patient.getBirthday(), showMonth);
            this.model.setPatientAge(age);

            if (patient.getSimpleAddressModel()!=null) {
                this.model.setPatientAddress(patient.getSimpleAddressModel().getAddress());
            }
            this.model.setPatientTelephone(patient.getTelephone());

            // 紹介元
            UserModel user = Project.getUserModel();
            this.model.setConsultantHospital(user.getFacilityModel().getFacilityName());
            this.model.setConsultantDoctor(user.getCommonName());
            this.model.setConsultantDept(user.getDepartmentModel().getDepartmentDesc());
            this.model.setConsultantTelephone(user.getFacilityModel().getTelephone());
            this.model.setConsultantFax(user.getFacilityModel().getFacsimile());
            this.model.setConsultantZipCode(user.getFacilityModel().getZipCode());
            this.model.setConsultantAddress(user.getFacilityModel().getAddress());

            // 来院日
            String value = LetterHelper.getDateAsString(new Date(),"yyyy-MM-dd");
            LetterItem item = new LetterItem(ITEM_VISITED_DATE, value);
            model.addLetterItem(item);
        }

        // view を生成
        this.view = new Reply2View();
// minagawa 中央へ位置するように変更 ^        
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.add(this.view);
        JScrollPane scroller = new JScrollPane(p);
        getUI().setLayout(new BorderLayout());
        getUI().add(scroller, BorderLayout.CENTER);
//        JScrollPane scroller = new JScrollPane(this.view);
//        getUI().setLayout(new BorderLayout());
//        getUI().add(scroller);
// minagawa $        

        modelToView(this.model);
        setEditables(true);
        setListeners();
        
//minagawa^ LSC 1.4 bug fix : Mac JDK7 bug マックの上下キー問題 2013/06/24
        if (ClientContext.isMac()) {
            new MacInputFixer().fix(view.getInformedContent());
        }
//minagawa$        
        
        stateMgr = new LetterStateMgr(this);
//minagawa^ LSC Test        
        this.enter();
//minagawa$        
        
//s.oh^ 文書の必要事項対応
        //view.getClientHospital().setBackground(Color.YELLOW);
        //view.getInformedContent().setBackground(Color.YELLOW);
//s.oh$
    }

    @Override
    public void stop() {
    }

    @Override
    public void save() {

        viewToModel(true);

        DBTask task = new DBTask<Boolean, Void>(getContext()) {

            @Override
            protected Boolean doInBackground() throws Exception {

                LetterDelegater ddl = new LetterDelegater();
                long result = ddl.saveOrUpdateLetter(model);
                model.setId(result);
                return true;
            }

            @Override
            protected void succeeded(Boolean result) {
//minagawa^ Chartの close box 押下で保存する場合、保存終了を通知しておしまい。                    
            if (boundSupport!=null) {
                Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_INFORMATION, "ご報告", "保存成功", "インスペクタの終了");
                setChartDocDidSave(true);
                return;
            }
//minagawa$                 
// minagawa 紹介状等の履歴に遷移 ^                
                getContext().getDocumentHistory().getLetterHistory();
//                getContext().getDocumentHistory().getDocumentHistory();
// minagawa $
                stateMgr.processSavedEvent();
                
                Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_INFORMATION, "ご報告", "保存成功");
            }
        };

        task.execute();
    }

    @Override
    public void enter() {
        super.enter();
        if (stateMgr != null) {
            stateMgr.enter();
        }
    }

    @Override
    public void print() {
        
        if (this.model == null) {
            return;
        }
        
        viewToModel(false);

        StringBuilder sb = new StringBuilder();
        sb.append("PDFファイルを作成しますか?");

        int option = JOptionPane.showOptionDialog(
                getContext().getFrame(),
                sb.toString(),
                ClientContext.getFrameTitle("御報告書印刷"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"PDF作成", "フォーム印刷", GUIFactory.getCancelButtonText()},
                "PDF作成");
        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_OTHER, ClientContext.getFrameTitle("御報告書印刷"), sb.toString());

        if (option == 0) {
            Log.outputOperLogDlg(getContext(), Log.LOG_LEVEL_0, "PDF作成");
            makePDF();
        } else if (option == 1) {
            Log.outputOperLogDlg(getContext(), Log.LOG_LEVEL_0, "フォーム印刷");
            PageFormat pageFormat = getContext().getContext().getPageFormat();
            String name = getContext().getPatient().getFullName();
            Panel2 panel = (Panel2) this.view;
            panel.printPanel(pageFormat, 1, false, name, 0, true);
        }else{
            Log.outputOperLogDlg(getContext(), Log.LOG_LEVEL_0, "取消し");
        }
    }

    @Override
    public void makePDF() {

        if (this.model == null) {
            return;
        }
                
        SwingWorker w = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                Reply2PDFMaker pdf = new Reply2PDFMaker();
                pdf.setDocumentDir(Project.getString(Project.LOCATION_PDF));
                pdf.setModel(model);
                return pdf.create();
            }
            
            @Override
            protected void done() {
                String err = null;
                try {
//minagawa^ jdk7                   
                    //String pathToPDF = get();
                    //Desktop.getDesktop().open(new File(pathToPDF));
                    URI uri = Paths.get(get()).toUri();
                    Desktop.getDesktop().browse(uri);
//minagawa$         
                } catch (IOException ex) {
                    err = "PDFファイルに関連づけされたアプリケーションを起動できません。";
                } catch (InterruptedException ex) {
                } catch (Throwable ex) {
                    err = ex.getMessage();
                }
                
                if (err!=null) {
                    Window parent = SwingUtilities.getWindowAncestor(getContext().getFrame());
                    JOptionPane.showMessageDialog(parent, err, ClientContext.getFrameTitle("PDF作成"), JOptionPane.WARNING_MESSAGE);
                    Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_WARNING, ClientContext.getFrameTitle("PDF作成"), err);
                }
            }
        };
        
        w.execute();
    }

    @Override
    public boolean isDirty() {
        if (stateMgr != null) {
            return stateMgr.isDirtyState();
        } else {
            return super.isDirty();
        }
    }

//minagawa^ LSC Test
//    public void modifyKarte() {
//        stateMgr.processModifyKarteEvent();
//    }
//minagawa$
    
    @Override
    public void setEditables(boolean b) {
        view.getClientHospital().setEditable(b);
        view.getClientDept().setEditable(b);
        view.getClientDoctor().setEditable(b);
        view.getInformedContent().setEditable(b);
    }

    @Override
    public void setListeners() {

        if (listenerIsAdded) {
            return;
        }

        DocumentListener dl = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                stateMgr.processDirtyEvent();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                stateMgr.processDirtyEvent();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                stateMgr.processDirtyEvent();
            }
        };

        ChartMediator med = getContext().getChartMediator();

        // 紹介元（宛先）病院
        view.getClientHospital().getDocument().addDocumentListener(dl);
        view.getClientHospital().addFocusListener(AutoKanjiListener.getInstance());
        view.getClientHospital().setTransferHandler(new BundleTransferHandler(med, view.getClientHospital()));
        view.getClientHospital().addMouseListener(CutCopyPasteAdapter.getInstance());

        // 紹介元（宛先）診療化
        view.getClientDept().getDocument().addDocumentListener(dl);
        view.getClientDept().addFocusListener(AutoKanjiListener.getInstance());
        view.getClientDept().setTransferHandler(new BundleTransferHandler(med, view.getClientDept()));
        view.getClientDept().addMouseListener(CutCopyPasteAdapter.getInstance());

        // 紹介元（宛先）医師
        view.getClientDoctor().getDocument().addDocumentListener(dl);
        view.getClientDoctor().addFocusListener(AutoKanjiListener.getInstance());
        view.getClientDoctor().setTransferHandler(new BundleTransferHandler(med, view.getClientDoctor()));
        view.getClientDoctor().addMouseListener(CutCopyPasteAdapter.getInstance());

        // 来院日
        PopupCalendarListener pl = new PopupCalendarListener(view.getVisited());
        view.getVisited().getDocument().addDocumentListener(dl);

        // Informed
        view.getInformedContent().getDocument().addDocumentListener(dl);
        view.getInformedContent().addFocusListener(AutoKanjiListener.getInstance());
        view.getInformedContent().setTransferHandler(new BundleTransferHandler(med, view.getInformedContent()));
        view.getInformedContent().addMouseListener(CutCopyPasteAdapter.getInstance());

        listenerIsAdded = true;
    }

    @Override
    public boolean letterIsDirty() {
//minagawa^ LSC Test        
        boolean dirty = (LetterHelper.getFieldValue(view.getClientHospital()) != null);
        dirty = dirty || (LetterHelper.getFieldValue(view.getVisited()) != null);
        dirty = dirty || (LetterHelper.getAreaValue(view.getInformedContent()) != null);   
        dirty = dirty || (LetterHelper.getFieldValue(view.getClientDept())!=null);
        dirty = dirty || (LetterHelper.getFieldValue(view.getClientDoctor())!=null);
        return dirty;
//minagawa$        
    }
}
