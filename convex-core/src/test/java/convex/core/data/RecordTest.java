package convex.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import convex.core.Belief;
import convex.core.init.InitTest;
import convex.core.lang.impl.RecordFormat;

public class RecordTest {

	@Test
	public void testBelief() {
		Belief b=Belief.createSingleOrder(InitTest.FIRST_PEER_KEYPAIR);
		assertEquals(b.getRefCount(),b.getOrders().getRefCount());

		doRecordTests(b);

	}

	public static void doRecordTests(ARecord r) {

		RecordFormat format=r.getFormat();

		AVector<Keyword> keys=format.getKeys();
		int n=(int) keys.count();

		AVector<ACell> vals=r.values();
		assertEquals(n,vals.size());

		ACell[] vs=new ACell[n]; // new array to extract values
		for (int i=0; i<n; i++) {
			// standard element access by key
			Keyword k=keys.get(i);
			ACell v=r.get(k);
			vs[i]=v;

			// entry based access by key
			MapEntry<Keyword,ACell> me0=r.getEntry(k);
			assertEquals(k,me0.getKey());
			assertEquals(v,me0.getValue());

			// TODO: consider this invariant?
			assertEquals(r.toHashMap(),r.assoc(k, v));

			// indexed access
			assertEquals(v,vals.get(i));

			// indexed entry-wise access
			MapEntry<Keyword,ACell> me=r.entryAt(i);
			assertEquals(k,me.getKey());
			assertEquals(v,me.getValue());
		}
		assertThrows(IndexOutOfBoundsException.class,()->r.entryAt(n));
		assertThrows(IndexOutOfBoundsException.class,()->r.entryAt(-1));

		int rc=r.getRefCount();
		for (int i=0; i<rc; i++) {
			r.getRef(i);
		}
		assertThrows(Exception.class,()->r.getRef(rc));

		assertSame(r,r.updateAll(r.getValuesArray()));
		assertSame(r,r.updateAll(r.values().toCellArray()));

		CollectionsTest.doDataStructureTests(r);
	}
}
