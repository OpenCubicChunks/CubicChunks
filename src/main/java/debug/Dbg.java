package debug;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Created by bartosz on 11/23/16.
 */
public class Dbg {
	private static final PrintWriter pw;

	static {
		PrintWriter p = null;
		try {
			p = new PrintWriter(new File("DEBUG" + new Date()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		pw = p;
		Runtime.getRuntime().addShutdownHook(new Thread(pw::close));
	}

	public static void p(String format, Object... objs) {
		if (FMLCommonHandler.instance().getEffectiveSide() != Side.CLIENT)
			pw.printf(format, objs);
	}

	public static void l(String format, Object... objs) {
		p(format, objs);
		pw.println();
	}


}
