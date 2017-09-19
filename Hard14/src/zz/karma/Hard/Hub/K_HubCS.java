package zz.karma.Hard.Hub;

import azura.karma.run.Karma;
import azura.karma.run.KarmaReaderA;
import azura.karma.def.KarmaSpace;

/**
*@note empty
*/
public class K_HubCS extends KarmaReaderA {
	public static final int type = 18389993;

	public K_HubCS(KarmaSpace space) {
		super(space, type , 18912909);
	}

	@Override
	public void fromKarma(Karma karma) {
		send = karma.getKarma(0);
	}

	@Override
	public Karma toKarma() {
		karma.setKarma(0, send);
		return karma;
	}

	/**
	*@type KARMA
	*<p>[CustomMsg] empty
	*<p>[HardMsgCS] empty
	*@note empty
	*/
	public Karma send;

	public static final int T_CustomMsg = 18389991;
	public static final int T_HardMsgCS = 18406023;
}