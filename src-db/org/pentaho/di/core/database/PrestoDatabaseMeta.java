package org.pentaho.di.core.database;

import java.lang.reflect.Constructor;
import java.sql.Driver;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.plugins.DatabaseMetaPlugin;
import org.pentaho.di.core.row.ValueMetaInterface;

@DatabaseMetaPlugin(type="Presto", typeDescription="Presto")
public class PrestoDatabaseMeta extends BaseDatabaseMeta
  implements DatabaseInterface
{
  protected static final String JAR_FILE = "presto-jdbc-0.146.jar";
  protected static final String DRIVER_CLASS_NAME = "com.facebook.presto.jdbc.PrestoDriver";
  protected static final int DEFAULT_PORT = 8080;
  protected Integer driverMajorVersion;
  protected Integer driverMinorVersion;

  public PrestoDatabaseMeta()
    throws Throwable
  {
  }

  PrestoDatabaseMeta(int majorVersion, int minorVersion)
    throws Throwable
  {
    this.driverMajorVersion = Integer.valueOf(majorVersion);
    this.driverMinorVersion = Integer.valueOf(minorVersion);
  }

  public int[] getAccessTypeList()
  {
    return new int[] { 0 };
  }

  public String getAddColumnStatement(String tablename, ValueMetaInterface v, String tk, boolean useAutoinc, String pk, boolean semicolon)
  {
    return "ALTER TABLE " + tablename + " ADD " + getFieldDefinition(v, tk, pk, useAutoinc, true, false);
  }

  public String getDriverClass()
  {
    return DRIVER_CLASS_NAME;
  }

  public String getFieldDefinition(ValueMetaInterface v, String tk, String pk, boolean useAutoinc, boolean addFieldname, boolean addCr)
  {
    String retval = "";

    String fieldname = v.getName();
    int length = v.getLength();
    int precision = v.getPrecision();

    if (addFieldname) {
      retval = retval + fieldname + " ";
    }

    int type = v.getType();
    switch (type)
    {
    case 4:
      retval = retval + "BOOLEAN";
      break;
    case 3:
      retval = retval + "STRING";
      break;
    case 2:
      retval = retval + "STRING";
      break;
    case 1:
    case 5:
    case 6:
      if (precision == 0) {
        if (length > 9) {
          if (length < 19)
          {
            retval = retval + "BIGINT";
          }
          else {
            retval = retval + "FLOAT";
          }
        }
        else {
          retval = retval + "INT";
        }

      }
      else if (length > 15) {
        retval = retval + "FLOAT";
      }
      else
      {
        retval = retval + "DOUBLE";
      }

    }

    return retval;
  }

  public String getModifyColumnStatement(String tablename, ValueMetaInterface v, String tk, boolean useAutoinc, String pk, boolean semicolon)
  {
    return "ALTER TABLE " + tablename + " MODIFY " + getFieldDefinition(v, tk, pk, useAutoinc, true, false);
  }

  public String getURL(String hostname, String port, String databaseName)
    throws KettleDatabaseException
  {
    if (Const.isEmpty(port)) {
      Integer.toString(getDefaultDatabasePort());
    }

    return "jdbc:presto://" + hostname + ":" + port + "/" + databaseName;
  }

  public String[] getUsedLibraries()
  {
    return new String[] { JAR_FILE };
  }

  public String getSelectCountStatement(String tableName)
  {
    return "select count(1) from " + tableName;
  }

  public String generateColumnAlias(int columnIndex, String suggestedName)
  {
    if (isDriverVersion(0, 6)) {
      return suggestedName;
    }

    return "_col" + String.valueOf(columnIndex);
  }

  protected synchronized void initDriverInfo()
  {
    Integer majorVersion = Integer.valueOf(0);
    Integer minorVersion = Integer.valueOf(0);
    try
    {
      Class driverClass = Class.forName(DRIVER_CLASS_NAME);
      if (driverClass != null) {
        Driver driver = (Driver)driverClass.getConstructor(new Class[0]).newInstance(new Object[0]);
        majorVersion = Integer.valueOf(driver.getMajorVersion());
        minorVersion = Integer.valueOf(driver.getMinorVersion());
      }
    }
    catch (Exception e)
    {
    }
    this.driverMajorVersion = majorVersion;
    this.driverMinorVersion = minorVersion;
  }

  protected boolean isDriverVersion(int majorVersion, int minorVersion)
  {
    if (this.driverMajorVersion == null) {
      initDriverInfo();
    }

    if (majorVersion < this.driverMajorVersion.intValue())
    {
      return true;
    }if (majorVersion == this.driverMajorVersion.intValue())
    {
      if (minorVersion <= this.driverMinorVersion.intValue())
      {
        return true;
      }
    }

    return false;
  }

  public String getStartQuote()
  {
    return "";
  }

  public String getEndQuote()
  {
    return "";
  }

  public int getDefaultDatabasePort()
  {
    return DEFAULT_PORT;
  }

  public String[] getTableTypes()
  {
    return null;
  }

  public String[] getViewTypes()
  {
    return new String[] { "VIEW", "VIRTUAL_VIEW" };
  }

  public String getTruncateTableStatement(String tableName)
  {
    return null;
  }

  public boolean supportsSetCharacterStream()
  {
    return false;
  }

  public boolean supportsBatchUpdates()
  {
    return false;
  }
}