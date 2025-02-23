package convex.gui.manager.mainpanels.actors;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import convex.core.Result;
import convex.core.State;
import convex.core.data.ACell;
import convex.core.data.Address;
import convex.core.data.MapEntry;
import convex.core.lang.RT;
import convex.core.lang.Reader;
import convex.core.util.Utils;
import convex.gui.components.ActionPanel;
import convex.gui.components.CodeLabel;
import convex.gui.components.DefaultReceiveAction;
import convex.gui.components.Toast;
import convex.gui.components.models.OracleTableModel;
import convex.gui.manager.PeerGUI;
import convex.gui.manager.mainpanels.WalletPanel;
import convex.gui.utils.Toolkit;

@SuppressWarnings("serial")
public class OraclePanel extends JPanel {

	public static final Logger log = LoggerFactory.getLogger(OraclePanel.class.getName());

	Address oracleAddress = PeerGUI.getLatestState().lookupCNS("convex.trusted-oracle");

	OracleTableModel tableModel = new OracleTableModel(PeerGUI.getLatestState(), oracleAddress);
	JTable table = new JTable(tableModel);

	JScrollPane scrollPane = new JScrollPane(table);;

	long key = 1;

	public OraclePanel() {
		this.setLayout(new BorderLayout());

		// ===========================================
		// Top label
		add(new CodeLabel("Oracle at address: " + oracleAddress), BorderLayout.NORTH);

		// ===========================================
		// Central table
		PeerGUI.getStateModel().addPropertyChangeListener(pc -> {
			State newState = (State) pc.getNewValue();
			tableModel.setState(newState);
		});

		// Column layouts
		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setHorizontalAlignment(JLabel.LEFT);
		table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
		table.getColumnModel().getColumn(0).setPreferredWidth(80);
		table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
		table.getColumnModel().getColumn(1).setPreferredWidth(300);
		table.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);
		table.getColumnModel().getColumn(2).setPreferredWidth(80);
		table.getColumnModel().getColumn(3).setCellRenderer(leftRenderer);
		table.getColumnModel().getColumn(3).setPreferredWidth(300);

		// fonts
		table.setFont(Toolkit.SMALL_MONO_FONT);
		table.getTableHeader().setFont(Toolkit.SMALL_MONO_FONT);

		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // useful in scroll pane
		scrollPane.getViewport().setBackground(null);
		add(scrollPane, BorderLayout.CENTER);

		// ============================================
		// Action buttons
		ActionPanel actionPanel = new ActionPanel();
		add(actionPanel, BorderLayout.SOUTH);

		JButton createButton = new JButton("Create...");
		actionPanel.add(createButton);
		createButton.addActionListener(e -> {
			String desc = JOptionPane.showInputDialog(this, "Enter Oracle description as plain text:");
			if ((desc == null) || (desc.isBlank())) return;

			ACell code = Reader.read("(call " + oracleAddress + " " + "(register " + (key++)
					+ "  {:desc \"" + desc + "\" :trust #{*address*}}))");
			execute(code);
		});

		JButton finaliseButton = new JButton("Finalise...");
		actionPanel.add(finaliseButton);
		finaliseButton.addActionListener(e -> {
			String value = JOptionPane.showInputDialog(this, "Enter final value:");
			if ((value == null) || (value.isBlank())) return;
			int ix = table.getSelectedRow();
			if (ix < 0) return;
			MapEntry<ACell, ACell> me = tableModel.getList().entryAt(ix);

			ACell code = Reader.read(
					"(call " + oracleAddress + " " + "(provide " + me.getKey() + " " + value + "))");
			execute(code);
		});

		JButton makeMarketButton = new JButton("Make Market");
		actionPanel.add(makeMarketButton);
		makeMarketButton.addActionListener(e -> {
			int ix = table.getSelectedRow();
			if (ix < 0) return;
			MapEntry<ACell, ACell> me = tableModel.getList().entryAt(ix);
			Object key = me.getKey();
			log.info("Making market: " + key);

			String opts = JOptionPane
					.showInputDialog("Enter a list of possible values (forms, may separate with commas)");
			if ((opts == null) || opts.isBlank()) {
				Toast.display(scrollPane, "Prediction market making cancelled", Toast.INFO);
				return;
			}
			String outcomeString = "[" + opts + "]";

			String actorCode;
			try {
				actorCode = Utils.readResourceAsString("actors/prediction-market.con");
				String source = "(let [pmc " + actorCode + " ] " + "(deploy (pmc " + " 0x"
						+ oracleAddress.toString() + " " + key + " " + outcomeString + ")))";
				ACell code = Reader.read(source);
				PeerGUI.execute(WalletPanel.HERO, code).thenAcceptAsync(createMarketAction);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
	}

	private void execute(ACell code) {
		PeerGUI.execute(WalletPanel.HERO, code).thenAcceptAsync(receiveAction);
	}

	private final Consumer<Result> createMarketAction = new Consumer<Result>() {
		protected void handleResult(Object m) {
			if (m instanceof Address) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// ignore
				}
				Address addr = (Address) m;
				MarketsPanel.marketList.addElement(addr);
				showResult("Prediction market deployed: " + addr);
			} else {
				String resultString = "Expected Address but got: " + m;
				log.warn(resultString);
				Toast.display(scrollPane, resultString, Toast.FAIL);
			}
		}

		protected void handleError(long id, Object code, Object msg) {
			showError(code,msg);
		}

		@Override
		public void accept(Result t) {
			if (t.isError()) {
				handleError(RT.jvm(t.getID()),t.getErrorCode(),t.getValue());
			} else {
				handleResult(t.getValue());
			}
		}

	};

	private final DefaultReceiveAction receiveAction = new DefaultReceiveAction(scrollPane);

	private void showError(Object code, Object msg) {
		String resultString = "Error executing transaction: " + code + " "+msg;
		log.info(resultString);
		Toast.display(scrollPane, resultString, Toast.FAIL);
	}

	private void showResult(Object v) {
		String resultString = "Transaction executed successfully\n" + "Result: " + Utils.toString(v);
		log.info(resultString);
		Toast.display(scrollPane, resultString, Toast.SUCCESS);
	}
}
