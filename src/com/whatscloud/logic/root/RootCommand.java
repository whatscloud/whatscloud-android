package com.whatscloud.logic.root;

import android.util.Log;

import com.whatscloud.config.debug.Logging;
import com.whatscloud.root.RootTools;
import com.whatscloud.root.execution.Command;

public class RootCommand
{
    public static void execute(String command) throws Exception
    {
        //--------------------------------
        // Execute the query
        //--------------------------------

        Command rootCommand = new Command(0, false, command)
        {
            @Override
            public void commandOutput(int id, String line)
            {}

            @Override
            public void commandTerminated(int id, String error)
            {
                //--------------------------------
                // Log error
                //--------------------------------

                Log.e(Logging.TAG_NAME, error);
            }

            @Override
            public void commandCompleted(int id, int exitCode)
            {}
        };

        //--------------------------------
        // Execute the command
        //--------------------------------

        RootTools.getShell(true).add(rootCommand);

        //--------------------------------
        // Wait for it...
        //--------------------------------

        waitForFinish(rootCommand);
    }

    public static void waitForFinish(Command command) throws Exception
    {
        //--------------------------------
        // Command not finished?
        //--------------------------------

        while (!command.isFinished())
        {
            //--------------------------------
            // Need to synchronize in order
            // to use wait()
            //--------------------------------

            synchronized (command)
            {
                //--------------------------------
                // Check again
                //--------------------------------

                if (!command.isFinished())
                {
                    //--------------------------------
                    // Wait for notifyAll or
                    // until the specified interval
                    //--------------------------------

                    command.wait(com.whatscloud.config.root.RootCommand.MIN_COMMAND_FINISHED_CHECK_INTERVAL);
                }
            }
        }
    }
}
