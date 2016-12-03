package common;

/**
 * Enumerates FTP commands
 */

public enum FTPCommand
{
    USER("USER"),
    PASS("PASS"),
    QUIT("QUIT");

    private String command;

    private FTPCommand(final String command)
    {
        this.command = command;
    }

    public String getCommand()
    {
        return command;
    }

    public static FTPCommand fromString(final String command)
    {
        for(final FTPCommand fTPCommand : FTPCommand.values())
        {
            if(fTPCommand.command.equals(command))
            {
                return fTPCommand;
            }
        }

        throw new IllegalArgumentException("Command " + command + " can not be casted into an FTPCommand");
    }
}