package org.pentaho.di.core.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;

//import com.aofei.kettleplugin.DB2Load.DB2LoadMeta;

public class ProcessUtil {

	public int runProcess(String command,List<String> parameter, String workingDir,Map<String,String> variables,LogChannelInterface log) throws IOException,
	InterruptedException, KettleFileException {
	List<String> commands = new ArrayList<String> ();
	String parameterLine = null;
	if(parameter!=null)
	{
		parameterLine=  "\" ";
		for(int i=0;i<parameter.size();i++)
		{
			parameterLine+=parameter.get(i);
		}
		parameterLine+=" \"";
	}
	
	if (Const.getOS().equals("Windows 95"))
	{
		commands.add("command.com");
		commands.add("/C");
		commands.add(command);
		if(parameterLine!=null)
			commands.add(parameterLine);
		
	} else if (Const.getOS().startsWith("Windows"))
	{
		commands.add("cmd");
		commands.add("/C");
		commands.add(command);
		if(parameterLine!=null)
			commands.add(parameterLine);
	} else
	{
		commands.add(command);
		if(parameter!=null)
			for(int i=0;i<parameter.size();i++)
			{
				commands.add(parameter.get(i));
			}	
	}
	ProcessBuilder procBuilder = new ProcessBuilder(commands);
	Map<String, String> env = procBuilder.environment();
	//String[] variables = listVariables();
	if(variables!=null)
		env.putAll(variables);	
	if(workingDir !=null)
	{
		File file = new File(workingDir);
		procBuilder.directory(file);
		//procBuilder.directory(new File("c:/"));
	}
	Process proc = procBuilder.start();

// any error message?
//	if(log!=null)
//	{
	StreamLogger errorLogger = new StreamLogger(log, proc.getErrorStream(),
			"(stderr)");
// any output?
	StreamLogger outputLogger = new StreamLogger(log,
			proc.getInputStream(), "(stdout)");
//	new Thread(errorLogger).start();
//	new Thread(outputLogger).start();
//	}
// kick them off


	proc.waitFor();
	int exitValue = proc.exitValue();

	// close the streams
	// otherwise you get "Too many open files, java.io.IOException" after a
	// lot of iterations
	proc.getErrorStream().close();
	proc.getOutputStream().close();
	return exitValue;
}
	
	public static void main(String[] args)
	{
		ProcessUtil procUtil = new ProcessUtil();
		 ArrayList<String> list = new ArrayList<String>();
		 list.add("aaa");
		try {
			int returnValue = procUtil.runProcess("mkdir bbb",list, null, null, null);
			System.out.println(returnValue);
		} catch (KettleFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
