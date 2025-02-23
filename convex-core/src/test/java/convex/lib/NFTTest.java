package convex.lib;

import static convex.test.Assertions.assertAssertError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import convex.core.Constants;
import convex.core.data.ACell;
import convex.core.data.Address;
import convex.core.data.Symbol;
import convex.core.init.InitTest;
import convex.core.lang.ACVMTest;
import convex.core.lang.Context;
import convex.core.lang.Reader;
import convex.core.lang.TestState;
import convex.core.util.Utils;
import convex.test.Assertions;
import convex.test.Testing;

public class NFTTest extends ACVMTest {

	public NFTTest() {
		super(loadNFT().getState());
	}

	private static final Symbol nSym=Symbol.create("nft");

	private static Context<?> loadNFT() {
		Context<?> ctx=Context.createFake(InitTest.STATE,InitTest.HERO);
		try {
			String importS="(import convex.nft-tokens :as "+nSym.getName()+")";
			ACell code=Reader.read(importS);
			ctx=ctx.run(code);
			Address nft=(Address) ctx.getResult();
			assertFalse(ctx.isExceptional());

			ctx=ctx.define(nSym, nft);
		} catch (Throwable e) {
			throw Utils.sneakyThrow(e);
		}
		ACell code=Reader.read("(import convex.asset :as asset)");
		ctx=ctx.run(code);
		return ctx;
	}

	private static final Context<?> ctx=loadNFT().fork();

	@Test public void testSetup() {
		assertTrue(ctx.lookupValue(nSym) instanceof Address);
	}

	@Test public void testOneAccount() {
		Context<?> c=Testing.runTests(ctx,"contracts/nft/test1.con");
		Assertions.assertNotError(c);
	}

	@Test public void testTwoAccounts() {
		Context<?> c=ctx.fork();
		assertEquals(0L,ctx.getDepth());
		// set up p2 as a zombie account
		c=step(c,"(def p2 (address "+VILLAIN+"))");
		c=TestState.stepAs(VILLAIN,c,"(do "
				+ "(import convex.asset :as asset)\r\n"
				+ "(import convex.nft-tokens :as nft)\r\n"
				+ "(set-controller "+HERO+"))");

		c=c.withJuice(Constants.MAX_TRANSACTION_JUICE); //ensure enough juice
		c=Testing.runTests(c,"contracts/nft/test2.con");
		Assertions.assertNotError(c);

		assertAssertError(step(c,"(do\r\n"
				+ "  (def t1 (call nft (create-token nil nil)))\r\n"
				+ "  (asset/transfer nft [nft t1] nil)\r\n"
				+ "  (asset/offer nft [nft t1]))"));
	}
}
