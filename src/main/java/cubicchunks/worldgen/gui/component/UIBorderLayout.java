package cubicchunks.worldgen.gui.component;

import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.component.UIComponent;

import cubicchunks.worldgen.gui.ExtraGui;

public class UIBorderLayout extends UILayout<UIBorderLayout, UIBorderLayout.Border> {

	private int currentBorder = 0;

	public UIBorderLayout(ExtraGui gui) {
		super(gui);
	}

	@Override protected Border findNextLocation() {
		Border ret = Border.values()[currentBorder];
		currentBorder++;
		if (currentBorder >= Border.values().length) {
			currentBorder = Border.values().length - 1;
		}
		return ret;
	}

	@Override protected void layout() {
		this.locationToComponentMap().forEach((loc, comp) -> {
			comp.setAnchor(loc.getAnchor());
			comp.setPosition(0, 0);
		});
	}

	@Override protected void initLayout() {
	}

	@Override protected void onAdd(UIComponent<?> comp, Border at) {

	}

	@Override protected void onRemove(UIComponent<?> comp, Border at) {

	}

	public enum Border {
		//@formatter:off
		TOP_LEFT(-1, -1),   TOP(0, -1),     TOP_RIGHT(1, -1),
		LEFT(-1, 0),        CENTER(0, 0),   RIGHT(1, 0),
		BOTTOM_LEFT(-1, 1), BOTTOM(0, 1),   BOTTOM_RIGHT(1, 1);
		//@formatter:on

		private final int x;
		private final int y;

		Border(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int getAnchor() {
			int anchor = 0;
			if (x == -1) {
				anchor |= Anchor.LEFT;
			} else if (x == 0) {
				anchor |= Anchor.CENTER;
			} else {
				assert x == 1;
				anchor |= Anchor.RIGHT;
			}

			if (y == -1) {
				anchor |= Anchor.TOP;
			} else if (y == 0) {
				anchor |= Anchor.MIDDLE;
			} else {
				assert y == 1;
				anchor |= Anchor.BOTTOM;
			}

			return anchor;
		}
	}
}
