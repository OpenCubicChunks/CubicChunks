package cubicchunks.debug;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Prof {
	private static final Map<String, Long> counters = new ConcurrentHashMap<>();
	private static final Map<String, JLabel> counterLabels = new ConcurrentHashMap<>();
	private static volatile JFrame frame;
	private static volatile int y = 0;

	private static final int width = 800, height = 600;

	static {
		EventQueue.invokeLater(() -> {
			frame = new JFrame("CubeCacheClient - loaded chunks");
			frame.setSize(width, height);
			frame.setLayout(null);
			frame.setVisible(true);
			frame.setResizable(false);

			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent windowEvent) {
					frame.dispose();
					frame = null;
				}
			});

			new Thread("ProfGUI") {
				@Override
				public void run() {
					while (frame != null) {
						updateProf();
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
					throw new Error("done?");
				}
			}.start();
		});
	}

	private static void updateProf() {
		for (Map.Entry<String, Long> entry : counters.entrySet()) {
			String name = entry.getKey();
			JLabel label = getOrCreateLabel(name);
			label.setText(String.format("%s: %s", name, entry.getValue() + ""));
		}
		frame.repaint();
		frame.setSize(600, y+50);
	}

	private static JLabel getOrCreateLabel(String name) {
		if (!counterLabels.containsKey(name)) {
			JLabel label = new JLabel();
			label.setLocation(0, y);
			label.setSize(600, 14);
			frame.add(label);
			y += 15;
			counterLabels.put(name, label);
		}
		return counterLabels.get(name);
	}

	public static void call(String name) {
		if(!counters.containsKey(name)) {
			counters.put(name, 0L);
		}
		counters.put(name, counters.get(name) + 1L);
	}
}
