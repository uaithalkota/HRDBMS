package com.exascale.optimizer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import com.exascale.filesystem.RID;

public class RIDAndIndexKeys implements Serializable
{
	private static sun.misc.Unsafe unsafe;

	static
	{
		try
		{
			final Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (sun.misc.Unsafe)f.get(null);
		}
		catch (final Exception e)
		{
			unsafe = null;
		}
	}

	private RID rid;
	private ArrayList<ArrayList<Object>> indexKeys;

	public RIDAndIndexKeys(final RID rid, final ArrayList<ArrayList<Object>> indexKeys)
	{
		this.rid = rid;
		this.indexKeys = indexKeys;
	}

	public static RIDAndIndexKeys deserialize(final InputStream in, final HashMap<Long, Object> prev) throws Exception
	{
		final RIDAndIndexKeys value = (RIDAndIndexKeys)unsafe.allocateInstance(RIDAndIndexKeys.class);
		final int type = OperatorUtils.getType(in);
		if (type == 0)
		{
			return (RIDAndIndexKeys)OperatorUtils.readReference(in, prev);
		}

		if (type != 88)
		{
			throw new Exception("Corrupted stream. Expected type 88 but received " + type);
		}

		prev.put(OperatorUtils.readLong(in), value);
		final int node = OperatorUtils.readInt(in);
		final int device = OperatorUtils.readInt(in);
		final int page = OperatorUtils.readInt(in);
		final int recNum = OperatorUtils.readInt(in);
		value.rid = new RID(node, device, page, recNum);
		value.indexKeys = OperatorUtils.deserializeALALO(in, prev);
		return value;
	}

	@Override
	public boolean equals(final Object rhs)
	{
		if (rhs == null)
		{
			return false;
		}

		if (!(rhs instanceof RIDAndIndexKeys))
		{
			return false;
		}

		final RIDAndIndexKeys r = (RIDAndIndexKeys)rhs;
		return rid.equals(r.rid) && indexKeys.equals(r.indexKeys);
	}

	public ArrayList<ArrayList<Object>> getIndexKeys()
	{
		return indexKeys;
	}

	public RID getRID()
	{
		return rid;
	}

	@Override
	public int hashCode()
	{
		int hash = 17;
		hash = hash * 23 + rid.hashCode();
		hash = hash * 23 + indexKeys.hashCode();
		return hash;
	}

	public void serialize(final OutputStream out, final IdentityHashMap<Object, Long> prev) throws Exception
	{
		final Long id = prev.get(this);
		if (id != null)
		{
			OperatorUtils.serializeReference(id, out);
			return;
		}

		OperatorUtils.writeType(88, out);
		prev.put(this, OperatorUtils.writeID(out));
		OperatorUtils.writeInt(rid.getNode(), out);
		OperatorUtils.writeInt(rid.getDevice(), out);
		OperatorUtils.writeInt(rid.getBlockNum(), out);
		OperatorUtils.writeInt(rid.getRecNum(), out);
		OperatorUtils.serializeALALO(indexKeys, out, prev);
	}
}
