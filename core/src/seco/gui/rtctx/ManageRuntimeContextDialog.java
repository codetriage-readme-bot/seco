package seco.gui.rtctx;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle;

import org.hypergraphdb.HGHandle;

import seco.ThisNiche;
import seco.gui.GUIHelper;
import seco.gui.dialog.SecoDialog;
import seco.rtenv.RtU;
import seco.rtenv.RuntimeContext;
import seco.util.GUIUtil;

public class ManageRuntimeContextDialog extends SecoDialog
{
    private static final long serialVersionUID = 3567373905090160540L;

    public ManageRuntimeContextDialog()
    {
        super(GUIUtil.getFrame(), "Manage Runtime Context", false);
        if(GUIUtil.getFrame() == null) setIconImage(GUIHelper.LOGO_IMAGE);
        getContentPane().add(new ManageRtCtxPanel());
        setSize(280, 160);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE , 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });        
    };

    public static class ManageRtCtxPanel extends JPanel
    {
        private static final long serialVersionUID = 4968516173383266484L;
        private JList listCtx;
        private Map<String, RuntimeContext> map;

        public ManageRtCtxPanel()
        {
            initComponents();
        }

        private void initComponents()
        {
            listCtx = new JList();
            DefaultListModel model = new DefaultListModel();
            List<RuntimeContext> list = RtU.getAllRuntimeContexts();
            map = new HashMap<String, RuntimeContext>();
            for (RuntimeContext rc : list)
            {
                map.put(rc.getName(), rc);
                model.addElement(rc.getName());
            }
            listCtx.setModel(model);
            JScrollPane scroll = new JScrollPane(listCtx);
            JButton butEdit = new JButton("Edit");
            JButton butReboot = new JButton("Reboot");
            JButton butDelet = new JButton("Delete");

            butEdit.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    String name = (String) listCtx.getSelectedValue();
                    if (name == null) return;
                    EditRuntimeContextDialog dlg = new EditRuntimeContextDialog(
                            ManageRtCtxPanel.this.map.get(name));
                    GUIUtil.centerOnScreen(dlg);
                    dlg.setVisible(true);
                }
            });

            butReboot.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    if (listCtx.getSelectedValue() != null)
                    {
                        String name = (String) listCtx.getSelectedValue();
                        if (name == null) return;
                        HGHandle h = ThisNiche
                                .handleOf(ManageRtCtxPanel.this.map.get(name));
                        ThisNiche.getEvaluationContext(h).reboot();
                    }
                }
            });

            butDelet.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    String name = (String) listCtx.getSelectedValue();
                    if (name == null) return;
                    if (GUIUtil
                            .showConfirmDlg("The RuntimeContext will be permanently deleted. Are you sure?"))
                    {
                        HGHandle h = ThisNiche
                                .handleOf(ManageRtCtxPanel.this.map.get(name));
                        ThisNiche.graph.remove(h);
                        ManageRtCtxPanel.this.map.remove(name);
                    }
                }
            });

            GroupLayout layout = new GroupLayout(this);
            this.setLayout(layout);
            layout.setHorizontalGroup(layout.createParallelGroup(
                    GroupLayout.Alignment.LEADING).addGroup(
                    layout.createSequentialGroup()
                            .addComponent(scroll, GroupLayout.PREFERRED_SIZE,
                                    172, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(
                                    LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(
                                    layout.createParallelGroup(
                                            GroupLayout.Alignment.LEADING,
                                            false)
                                            .addComponent(butEdit,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE)
                                            .addComponent(butDelet,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE)
                                            .addComponent(butReboot,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE))
                            .addContainerGap(GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE)));
            layout.setVerticalGroup(layout
                    .createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(
                            layout.createSequentialGroup()
                                    .addComponent(butEdit)
                                    .addPreferredGap(
                                            LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(butReboot)
                                    .addPreferredGap(
                                            LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(butDelet))
                    .addComponent(scroll, GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE));            
        }
    }
}
