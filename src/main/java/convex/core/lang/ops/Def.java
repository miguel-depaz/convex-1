package convex.core.lang.ops;

import java.nio.ByteBuffer;

import convex.core.data.AMap;
import convex.core.data.Format;
import convex.core.data.IRefFunction;
import convex.core.data.Ref;
import convex.core.data.Symbol;
import convex.core.data.Syntax;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;
import convex.core.lang.AOp;
import convex.core.lang.Context;
import convex.core.lang.Juice;
import convex.core.lang.Ops;
import convex.core.util.Errors;
import convex.core.util.Utils;

/**
 * Op that creates a definition in the current environment.
 * 
 * Def may optionally have symbolic metadata attached to the symbol.
 * 
 * @param <T> Type of defined value
 */
public class Def<T> extends AOp<T> {

	// symbol Syntax Object including metadata to add to the defined environment
	private final Syntax symbol;
	
	// expression to execute to determine the defined value
	private final Ref<AOp<T>> op;

	private Def(Syntax key, Ref<AOp<T>> op) {
		this.op = op;
		this.symbol = key;
	}

	public static <T> Def<T> create(Syntax key, Ref<AOp<T>> op) {
		return new Def<T>(key, op);
	}

	public static <T> Def<T> create(Syntax key, AOp<T> op) {
		return create(key, Ref.create(op));
	}

	public static <T> Def<T> create(Symbol key, AOp<T> op) {
		return create(Syntax.create(key), Ref.create(op));
	}

	public static <T> Def<T> create(String key, AOp<T> op) {
		return create(Symbol.create(key), op);
	}

	@Override
	public <I> Context<T> execute(Context<I> context) {
		Context<T> opContext = (Context<T>) context.execute(op.getValue());
		if (opContext.isExceptional()) return opContext;
		T opResult = opContext.getResult();

		// TODO: defined syntax metadata
		opContext = opContext.define(symbol.getValue(), Syntax.create(opResult).withMeta(symbol.getMeta()));
		return opContext.withResult(Juice.DEF, opResult);
	}

	@Override
	public int getRefCount() {
		return 1;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Ref<AOp<T>> getRef(int i) {
		if (i != 0) throw new IndexOutOfBoundsException(Errors.badIndex(i));
		return op;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Def<T> updateRefs(IRefFunction func) {
		Ref<AOp<T>> newRef = (Ref<AOp<T>>) func.apply(op);
		if (op == newRef) return this;
		return create(symbol, newRef);
	}

	@Override
	public void ednString(StringBuilder sb) {
		sb.append("(def ");
		sb.append(symbol);
		sb.append(' ');
		Utils.ednString(sb, op.getValue());
		sb.append(')');
	}

	@Override
	public byte opCode() {
		return Ops.DEF;
	}

	@Override
	public ByteBuffer writeRaw(ByteBuffer b) {
		b = Format.write(b, symbol);
		b = op.write(b);
		return b;
	}

	public static <T> Def<T> read(ByteBuffer b) throws BadFormatException {
		Syntax symbol = Format.read(b);
		Ref<AOp<T>> ref = Format.readRef(b);
		return create(symbol, ref);
	}

	@Override
	public AOp<T> specialise(AMap<Symbol, Object> binds) {
		AOp<T> old = op.getValue();
		AOp<T> neww = old.specialise(binds);
		if (old == neww) return this;
		return new Def<T>(symbol, Ref.create(neww));
	}

	@Override
	public void validateCell() throws InvalidDataException {
		symbol.validateCell();
	}

}
