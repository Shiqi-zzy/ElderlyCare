package com.elderlycare.app.data.binding;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BindingDatabase_Impl extends BindingDatabase {
  private volatile BindingDao _bindingDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `organization` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `binding_request` (`id` TEXT NOT NULL, `requesterUserId` TEXT NOT NULL, `requesterRole` TEXT NOT NULL, `organizationId` TEXT NOT NULL, `familyUserId` TEXT NOT NULL, `elderlyId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `status` TEXT NOT NULL, `message` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `reviewedAt` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_binding_request_familyUserId` ON `binding_request` (`familyUserId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_binding_request_elderlyId` ON `binding_request` (`elderlyId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_binding_request_requesterUserId` ON `binding_request` (`requesterUserId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_elderly_binding` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `userRole` TEXT NOT NULL, `organizationId` TEXT NOT NULL, `elderlyId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `permission` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_elderly_binding_userId` ON `user_elderly_binding` (`userId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_elderly_binding_elderlyId` ON `user_elderly_binding` (`elderlyId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `local_alert` (`id` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `elderlyId` TEXT NOT NULL, `type` TEXT NOT NULL, `level` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_alert_deviceId` ON `local_alert` (`deviceId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_alert_elderlyId` ON `local_alert` (`elderlyId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '82442135824be248c6bed95aab0939d2')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `organization`");
        db.execSQL("DROP TABLE IF EXISTS `binding_request`");
        db.execSQL("DROP TABLE IF EXISTS `user_elderly_binding`");
        db.execSQL("DROP TABLE IF EXISTS `local_alert`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsOrganization = new HashMap<String, TableInfo.Column>(4);
        _columnsOrganization.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrganization.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrganization.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOrganization.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOrganization = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOrganization = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOrganization = new TableInfo("organization", _columnsOrganization, _foreignKeysOrganization, _indicesOrganization);
        final TableInfo _existingOrganization = TableInfo.read(db, "organization");
        if (!_infoOrganization.equals(_existingOrganization)) {
          return new RoomOpenHelper.ValidationResult(false, "organization(com.elderlycare.app.data.binding.OrganizationEntity).\n"
                  + " Expected:\n" + _infoOrganization + "\n"
                  + " Found:\n" + _existingOrganization);
        }
        final HashMap<String, TableInfo.Column> _columnsBindingRequest = new HashMap<String, TableInfo.Column>(12);
        _columnsBindingRequest.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("requesterUserId", new TableInfo.Column("requesterUserId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("requesterRole", new TableInfo.Column("requesterRole", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("organizationId", new TableInfo.Column("organizationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("familyUserId", new TableInfo.Column("familyUserId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("elderlyId", new TableInfo.Column("elderlyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("deviceId", new TableInfo.Column("deviceId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("message", new TableInfo.Column("message", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBindingRequest.put("reviewedAt", new TableInfo.Column("reviewedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBindingRequest = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBindingRequest = new HashSet<TableInfo.Index>(3);
        _indicesBindingRequest.add(new TableInfo.Index("index_binding_request_familyUserId", false, Arrays.asList("familyUserId"), Arrays.asList("ASC")));
        _indicesBindingRequest.add(new TableInfo.Index("index_binding_request_elderlyId", false, Arrays.asList("elderlyId"), Arrays.asList("ASC")));
        _indicesBindingRequest.add(new TableInfo.Index("index_binding_request_requesterUserId", false, Arrays.asList("requesterUserId"), Arrays.asList("ASC")));
        final TableInfo _infoBindingRequest = new TableInfo("binding_request", _columnsBindingRequest, _foreignKeysBindingRequest, _indicesBindingRequest);
        final TableInfo _existingBindingRequest = TableInfo.read(db, "binding_request");
        if (!_infoBindingRequest.equals(_existingBindingRequest)) {
          return new RoomOpenHelper.ValidationResult(false, "binding_request(com.elderlycare.app.data.binding.BindingRequestEntity).\n"
                  + " Expected:\n" + _infoBindingRequest + "\n"
                  + " Found:\n" + _existingBindingRequest);
        }
        final HashMap<String, TableInfo.Column> _columnsUserElderlyBinding = new HashMap<String, TableInfo.Column>(10);
        _columnsUserElderlyBinding.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("userRole", new TableInfo.Column("userRole", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("organizationId", new TableInfo.Column("organizationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("elderlyId", new TableInfo.Column("elderlyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("deviceId", new TableInfo.Column("deviceId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("permission", new TableInfo.Column("permission", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserElderlyBinding.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserElderlyBinding = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserElderlyBinding = new HashSet<TableInfo.Index>(2);
        _indicesUserElderlyBinding.add(new TableInfo.Index("index_user_elderly_binding_userId", false, Arrays.asList("userId"), Arrays.asList("ASC")));
        _indicesUserElderlyBinding.add(new TableInfo.Index("index_user_elderly_binding_elderlyId", false, Arrays.asList("elderlyId"), Arrays.asList("ASC")));
        final TableInfo _infoUserElderlyBinding = new TableInfo("user_elderly_binding", _columnsUserElderlyBinding, _foreignKeysUserElderlyBinding, _indicesUserElderlyBinding);
        final TableInfo _existingUserElderlyBinding = TableInfo.read(db, "user_elderly_binding");
        if (!_infoUserElderlyBinding.equals(_existingUserElderlyBinding)) {
          return new RoomOpenHelper.ValidationResult(false, "user_elderly_binding(com.elderlycare.app.data.binding.UserElderlyBindingEntity).\n"
                  + " Expected:\n" + _infoUserElderlyBinding + "\n"
                  + " Found:\n" + _existingUserElderlyBinding);
        }
        final HashMap<String, TableInfo.Column> _columnsLocalAlert = new HashMap<String, TableInfo.Column>(8);
        _columnsLocalAlert.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalAlert.put("deviceId", new TableInfo.Column("deviceId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalAlert.put("elderlyId", new TableInfo.Column("elderlyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalAlert.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalAlert.put("level", new TableInfo.Column("level", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalAlert.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalAlert.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalAlert.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocalAlert = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocalAlert = new HashSet<TableInfo.Index>(2);
        _indicesLocalAlert.add(new TableInfo.Index("index_local_alert_deviceId", false, Arrays.asList("deviceId"), Arrays.asList("ASC")));
        _indicesLocalAlert.add(new TableInfo.Index("index_local_alert_elderlyId", false, Arrays.asList("elderlyId"), Arrays.asList("ASC")));
        final TableInfo _infoLocalAlert = new TableInfo("local_alert", _columnsLocalAlert, _foreignKeysLocalAlert, _indicesLocalAlert);
        final TableInfo _existingLocalAlert = TableInfo.read(db, "local_alert");
        if (!_infoLocalAlert.equals(_existingLocalAlert)) {
          return new RoomOpenHelper.ValidationResult(false, "local_alert(com.elderlycare.app.data.binding.LocalAlertEntity).\n"
                  + " Expected:\n" + _infoLocalAlert + "\n"
                  + " Found:\n" + _existingLocalAlert);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "82442135824be248c6bed95aab0939d2", "dc3eb737a906adc32d4ac9192d549837");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "organization","binding_request","user_elderly_binding","local_alert");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `organization`");
      _db.execSQL("DELETE FROM `binding_request`");
      _db.execSQL("DELETE FROM `user_elderly_binding`");
      _db.execSQL("DELETE FROM `local_alert`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BindingDao.class, BindingDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public BindingDao bindingDao() {
    if (_bindingDao != null) {
      return _bindingDao;
    } else {
      synchronized(this) {
        if(_bindingDao == null) {
          _bindingDao = new BindingDao_Impl(this);
        }
        return _bindingDao;
      }
    }
  }
}
