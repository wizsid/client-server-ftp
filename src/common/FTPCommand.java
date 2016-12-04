package common;

/**
 * Enumerates FTP commands
 */
public enum FTPCommand
{
    CWD,
    DELE,
    EPSV,
    LIST,
    MDTM,
    MKD,
    MLSD,
    NLST,
    NOOP,
    PASS,
    PWD,
    QUIT,
    RETR,
    RMD,
    RNFR,
    RNTO,
    SIZE,
    STOR,
    TYPE,
    USER;

    public static FTPCommand fromUserInput(final String userInput)
    {
        for(final FTPCommand fTPCommand : FTPCommand.values())
        {
            if(userInput.startsWith(fTPCommand.name()))
            {
                return fTPCommand;
            }
        }

        throw new IllegalArgumentException("User input " + userInput + " can not be casted into an FTPCommand");
    }
}