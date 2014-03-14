package com.exascale.optimizer.testing;

 

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

public final class NetworkSendMultipleOperator extends NetworkSendOperator
{
	protected final int id;
	protected final ConcurrentHashMap<Integer, CompressedSocket> connections = new ConcurrentHashMap<Integer, CompressedSocket>(Phase3.MAX_INCOMING_CONNECTIONS, 1.0f, Phase3.MAX_INCOMING_CONNECTIONS);
	protected final ConcurrentHashMap<Integer, OutputStream> outs = new ConcurrentHashMap<Integer, OutputStream>(Phase3.MAX_INCOMING_CONNECTIONS, 1.0f, Phase3.MAX_INCOMING_CONNECTIONS);
	protected ArrayList<Operator> parents = new ArrayList<Operator>();
	
	public NetworkSendMultipleOperator(int id, MetaData meta)
	{
		this.id = id;
		this.meta = meta;
	}
	
	public int getID()
	{
		return id;
	}
	
	public boolean setNumParents()
	{
		if (numpSet)
		{
			return false;
		}
		
		numpSet = true;
		numParents = parents.size();
		return true;
	}
	
	public boolean hasAllConnections()
	{
		System.out.println(this + " is expecting " + numParents + " connections and has " + outs.size());
		return outs.size() == numParents;
	}
	
	public void addConnection(int fromNode, CompressedSocket sock)
	{
		connections.put(fromNode, sock);
		try
		{
			outs.put(fromNode, new BufferedOutputStream(sock.getOutputStream()));
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public NetworkSendMultipleOperator clone()
	{
		NetworkSendMultipleOperator retval = new NetworkSendMultipleOperator(id, meta);
		retval.node = this.node;
		retval.numParents = numParents;
		retval.numpSet = numpSet;
		return retval;
	}
	
	public Operator parent()
	{
		System.out.println("NetworkSendMultipleOperator does not support parent()");
		Thread.dumpStack();
		System.exit(1);
		return null;
	}
	
	public ArrayList<Operator> parents()
	{
		return parents;
	}
	
	public void removeParent(Operator op)
	{
		parents.remove(op);
	}
	
	public void clearParent()
	{
		parents.clear();
	}
	
	public String toString()
	{
		return "NetworkSendMultipleOperator(" + node + ")";
	}
	
	public void registerParent(Operator op)
	{
		parents.add(op);
	}
	
	public synchronized void start() throws Exception 
	{
		started = true;
		child.start();
		Object o = child.next(this);
		while (!(o instanceof DataEndMarker))
		{
			byte[] obj = toBytes(o);
			for (OutputStream out : outs.values())
			{
				out.write(obj);
			}
			count++;
			o = child.next(this);
		}
		
		byte[] obj = toBytes(o);
		for (OutputStream out : outs.values())
		{
			out.write(obj);
			out.flush();
		}
		System.out.println("Wrote " + count + " rows");
		child.close();
		Thread.sleep(60 * 1000);
	}
	
	public void close() throws Exception
	{
		for (OutputStream out : outs.values())
		{
			try
			{
				out.close();
			}
			catch(Exception e)
			{}
		}
		
		for (CompressedSocket sock : connections.values())
		{
			try
			{
				sock.close();
			}
			catch(Exception e)
			{}
		}
	}
}
