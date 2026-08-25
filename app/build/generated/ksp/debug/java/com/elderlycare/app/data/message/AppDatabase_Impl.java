package com.elderlycare.app.data.message;

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
import com.elderlycare.app.data.community.CommunityDao;
import com.elderlycare.app.data.community.CommunityDao_Impl;
import com.elderlycare.app.data.hospital.HealthAdviceDao;
import com.elderlycare.app.data.hospital.HealthAdviceDao_Impl;
import com.elderlycare.app.data.hospital.MedicalFollowUpDao;
import com.elderlycare.app.data.hospital.MedicalFollowUpDao_Impl;
import com.elderlycare.app.data.reminder.RemindPlanDao;
import com.elderlycare.app.data.reminder.RemindPlanDao_Impl;
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
public final class AppDatabase_Impl extends AppDatabase {
  private volatile MessageDao _messageDao;

  private volatile RemindPlanDao _remindPlanDao;

  private volatile MedicalFollowUpDao _medicalFollowUpDao;

  private volatile HealthAdviceDao _healthAdviceDao;

  private volatile CommunityDao _communityDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(7) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `message` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `msgType` INTEGER NOT NULL, `senderName` TEXT NOT NULL, `content` TEXT NOT NULL, `localAudioPath` TEXT NOT NULL, `duration` INTEGER NOT NULL, `createTime` INTEGER NOT NULL, `isRead` INTEGER NOT NULL, `deviceSerial` TEXT NOT NULL, `remoteId` TEXT NOT NULL, `sendStatus` INTEGER NOT NULL, `sendChannel` INTEGER NOT NULL, `failReason` TEXT NOT NULL, `localVideoPath` TEXT NOT NULL DEFAULT '', `videoCloudUrl` TEXT NOT NULL DEFAULT '', `thumbUrl` TEXT NOT NULL DEFAULT '', `messageCategory` INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_deviceSerial` ON `message` (`deviceSerial`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_createTime` ON `message` (`createTime`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `remind_plan` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clockId` TEXT NOT NULL, `tag` TEXT NOT NULL, `content` TEXT NOT NULL, `timeHour` INTEGER NOT NULL, `timeMin` INTEGER NOT NULL, `repeatType` INTEGER NOT NULL, `weekdays` TEXT NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `day` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `executed` INTEGER NOT NULL, `deviceSerial` TEXT NOT NULL, `createTime` INTEGER NOT NULL, `source` INTEGER NOT NULL DEFAULT 0, `confirmStatus` INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_remind_plan_deviceSerial` ON `remind_plan` (`deviceSerial`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_remind_plan_clockId` ON `remind_plan` (`clockId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `medical_follow_up_record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `elderlyId` TEXT NOT NULL, `followUpTime` INTEGER NOT NULL, `content` TEXT NOT NULL, `status` TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medical_follow_up_record_elderlyId` ON `medical_follow_up_record` (`elderlyId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medical_follow_up_record_followUpTime` ON `medical_follow_up_record` (`followUpTime`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `health_advice` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `elderlyId` TEXT NOT NULL, `adviceTime` INTEGER NOT NULL, `adviceContent` TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_advice_elderlyId` ON `health_advice` (`elderlyId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_advice_adviceTime` ON `health_advice` (`adviceTime`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `community_follow_up` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `elderlyId` TEXT NOT NULL, `elderlyName` TEXT NOT NULL, `staffId` TEXT NOT NULL, `followUpType` TEXT NOT NULL, `scheduledTime` INTEGER NOT NULL, `content` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_community_follow_up_elderlyId` ON `community_follow_up` (`elderlyId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_community_follow_up_staffId` ON `community_follow_up` (`staffId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_community_follow_up_scheduledTime` ON `community_follow_up` (`scheduledTime`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `staff_schedule` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `staffId` TEXT NOT NULL, `title` TEXT NOT NULL, `scheduleDate` INTEGER NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `location` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_staff_schedule_staffId` ON `staff_schedule` (`staffId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_staff_schedule_scheduleDate` ON `staff_schedule` (`scheduleDate`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `service_record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `staffId` TEXT NOT NULL, `elderlyId` TEXT NOT NULL, `elderlyName` TEXT NOT NULL, `serviceType` TEXT NOT NULL, `content` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_record_staffId` ON `service_record` (`staffId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_record_elderlyId` ON `service_record` (`elderlyId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_record_createdAt` ON `service_record` (`createdAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `todo_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `staffId` TEXT NOT NULL, `elderlyId` TEXT NOT NULL, `elderlyName` TEXT NOT NULL, `todoType` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_item_staffId` ON `todo_item` (`staffId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_item_elderlyId` ON `todo_item` (`elderlyId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_item_status` ON `todo_item` (`status`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '21daeb030cc244bd4c7140b47679ee7c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `message`");
        db.execSQL("DROP TABLE IF EXISTS `remind_plan`");
        db.execSQL("DROP TABLE IF EXISTS `medical_follow_up_record`");
        db.execSQL("DROP TABLE IF EXISTS `health_advice`");
        db.execSQL("DROP TABLE IF EXISTS `community_follow_up`");
        db.execSQL("DROP TABLE IF EXISTS `staff_schedule`");
        db.execSQL("DROP TABLE IF EXISTS `service_record`");
        db.execSQL("DROP TABLE IF EXISTS `todo_item`");
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
        final HashMap<String, TableInfo.Column> _columnsMessage = new HashMap<String, TableInfo.Column>(17);
        _columnsMessage.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("msgType", new TableInfo.Column("msgType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("senderName", new TableInfo.Column("senderName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("localAudioPath", new TableInfo.Column("localAudioPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("createTime", new TableInfo.Column("createTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("isRead", new TableInfo.Column("isRead", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("deviceSerial", new TableInfo.Column("deviceSerial", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("remoteId", new TableInfo.Column("remoteId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("sendStatus", new TableInfo.Column("sendStatus", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("sendChannel", new TableInfo.Column("sendChannel", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("failReason", new TableInfo.Column("failReason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("localVideoPath", new TableInfo.Column("localVideoPath", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("videoCloudUrl", new TableInfo.Column("videoCloudUrl", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("thumbUrl", new TableInfo.Column("thumbUrl", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY));
        _columnsMessage.put("messageCategory", new TableInfo.Column("messageCategory", "INTEGER", true, 0, "1", TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessage = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessage = new HashSet<TableInfo.Index>(2);
        _indicesMessage.add(new TableInfo.Index("index_message_deviceSerial", false, Arrays.asList("deviceSerial"), Arrays.asList("ASC")));
        _indicesMessage.add(new TableInfo.Index("index_message_createTime", false, Arrays.asList("createTime"), Arrays.asList("ASC")));
        final TableInfo _infoMessage = new TableInfo("message", _columnsMessage, _foreignKeysMessage, _indicesMessage);
        final TableInfo _existingMessage = TableInfo.read(db, "message");
        if (!_infoMessage.equals(_existingMessage)) {
          return new RoomOpenHelper.ValidationResult(false, "message(com.elderlycare.app.data.message.MessageEntity).\n"
                  + " Expected:\n" + _infoMessage + "\n"
                  + " Found:\n" + _existingMessage);
        }
        final HashMap<String, TableInfo.Column> _columnsRemindPlan = new HashMap<String, TableInfo.Column>(17);
        _columnsRemindPlan.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("clockId", new TableInfo.Column("clockId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("tag", new TableInfo.Column("tag", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("timeHour", new TableInfo.Column("timeHour", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("timeMin", new TableInfo.Column("timeMin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("repeatType", new TableInfo.Column("repeatType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("weekdays", new TableInfo.Column("weekdays", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("year", new TableInfo.Column("year", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("month", new TableInfo.Column("month", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("day", new TableInfo.Column("day", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("executed", new TableInfo.Column("executed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("deviceSerial", new TableInfo.Column("deviceSerial", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("createTime", new TableInfo.Column("createTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("source", new TableInfo.Column("source", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        _columnsRemindPlan.put("confirmStatus", new TableInfo.Column("confirmStatus", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRemindPlan = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRemindPlan = new HashSet<TableInfo.Index>(2);
        _indicesRemindPlan.add(new TableInfo.Index("index_remind_plan_deviceSerial", false, Arrays.asList("deviceSerial"), Arrays.asList("ASC")));
        _indicesRemindPlan.add(new TableInfo.Index("index_remind_plan_clockId", false, Arrays.asList("clockId"), Arrays.asList("ASC")));
        final TableInfo _infoRemindPlan = new TableInfo("remind_plan", _columnsRemindPlan, _foreignKeysRemindPlan, _indicesRemindPlan);
        final TableInfo _existingRemindPlan = TableInfo.read(db, "remind_plan");
        if (!_infoRemindPlan.equals(_existingRemindPlan)) {
          return new RoomOpenHelper.ValidationResult(false, "remind_plan(com.elderlycare.app.data.reminder.RemindPlanEntity).\n"
                  + " Expected:\n" + _infoRemindPlan + "\n"
                  + " Found:\n" + _existingRemindPlan);
        }
        final HashMap<String, TableInfo.Column> _columnsMedicalFollowUpRecord = new HashMap<String, TableInfo.Column>(5);
        _columnsMedicalFollowUpRecord.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicalFollowUpRecord.put("elderlyId", new TableInfo.Column("elderlyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicalFollowUpRecord.put("followUpTime", new TableInfo.Column("followUpTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicalFollowUpRecord.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicalFollowUpRecord.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMedicalFollowUpRecord = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMedicalFollowUpRecord = new HashSet<TableInfo.Index>(2);
        _indicesMedicalFollowUpRecord.add(new TableInfo.Index("index_medical_follow_up_record_elderlyId", false, Arrays.asList("elderlyId"), Arrays.asList("ASC")));
        _indicesMedicalFollowUpRecord.add(new TableInfo.Index("index_medical_follow_up_record_followUpTime", false, Arrays.asList("followUpTime"), Arrays.asList("ASC")));
        final TableInfo _infoMedicalFollowUpRecord = new TableInfo("medical_follow_up_record", _columnsMedicalFollowUpRecord, _foreignKeysMedicalFollowUpRecord, _indicesMedicalFollowUpRecord);
        final TableInfo _existingMedicalFollowUpRecord = TableInfo.read(db, "medical_follow_up_record");
        if (!_infoMedicalFollowUpRecord.equals(_existingMedicalFollowUpRecord)) {
          return new RoomOpenHelper.ValidationResult(false, "medical_follow_up_record(com.elderlycare.app.data.hospital.MedicalFollowUpRecord).\n"
                  + " Expected:\n" + _infoMedicalFollowUpRecord + "\n"
                  + " Found:\n" + _existingMedicalFollowUpRecord);
        }
        final HashMap<String, TableInfo.Column> _columnsHealthAdvice = new HashMap<String, TableInfo.Column>(4);
        _columnsHealthAdvice.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthAdvice.put("elderlyId", new TableInfo.Column("elderlyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthAdvice.put("adviceTime", new TableInfo.Column("adviceTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthAdvice.put("adviceContent", new TableInfo.Column("adviceContent", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHealthAdvice = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHealthAdvice = new HashSet<TableInfo.Index>(2);
        _indicesHealthAdvice.add(new TableInfo.Index("index_health_advice_elderlyId", false, Arrays.asList("elderlyId"), Arrays.asList("ASC")));
        _indicesHealthAdvice.add(new TableInfo.Index("index_health_advice_adviceTime", false, Arrays.asList("adviceTime"), Arrays.asList("ASC")));
        final TableInfo _infoHealthAdvice = new TableInfo("health_advice", _columnsHealthAdvice, _foreignKeysHealthAdvice, _indicesHealthAdvice);
        final TableInfo _existingHealthAdvice = TableInfo.read(db, "health_advice");
        if (!_infoHealthAdvice.equals(_existingHealthAdvice)) {
          return new RoomOpenHelper.ValidationResult(false, "health_advice(com.elderlycare.app.data.hospital.HealthAdvice).\n"
                  + " Expected:\n" + _infoHealthAdvice + "\n"
                  + " Found:\n" + _existingHealthAdvice);
        }
        final HashMap<String, TableInfo.Column> _columnsCommunityFollowUp = new HashMap<String, TableInfo.Column>(10);
        _columnsCommunityFollowUp.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("elderlyId", new TableInfo.Column("elderlyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("elderlyName", new TableInfo.Column("elderlyName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("staffId", new TableInfo.Column("staffId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("followUpType", new TableInfo.Column("followUpType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("scheduledTime", new TableInfo.Column("scheduledTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommunityFollowUp.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCommunityFollowUp = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCommunityFollowUp = new HashSet<TableInfo.Index>(3);
        _indicesCommunityFollowUp.add(new TableInfo.Index("index_community_follow_up_elderlyId", false, Arrays.asList("elderlyId"), Arrays.asList("ASC")));
        _indicesCommunityFollowUp.add(new TableInfo.Index("index_community_follow_up_staffId", false, Arrays.asList("staffId"), Arrays.asList("ASC")));
        _indicesCommunityFollowUp.add(new TableInfo.Index("index_community_follow_up_scheduledTime", false, Arrays.asList("scheduledTime"), Arrays.asList("ASC")));
        final TableInfo _infoCommunityFollowUp = new TableInfo("community_follow_up", _columnsCommunityFollowUp, _foreignKeysCommunityFollowUp, _indicesCommunityFollowUp);
        final TableInfo _existingCommunityFollowUp = TableInfo.read(db, "community_follow_up");
        if (!_infoCommunityFollowUp.equals(_existingCommunityFollowUp)) {
          return new RoomOpenHelper.ValidationResult(false, "community_follow_up(com.elderlycare.app.data.community.CommunityFollowUpRecord).\n"
                  + " Expected:\n" + _infoCommunityFollowUp + "\n"
                  + " Found:\n" + _existingCommunityFollowUp);
        }
        final HashMap<String, TableInfo.Column> _columnsStaffSchedule = new HashMap<String, TableInfo.Column>(9);
        _columnsStaffSchedule.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStaffSchedule.put("staffId", new TableInfo.Column("staffId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStaffSchedule.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStaffSchedule.put("scheduleDate", new TableInfo.Column("scheduleDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStaffSchedule.put("startTime", new TableInfo.Column("startTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStaffSchedule.put("endTime", new TableInfo.Column("endTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStaffSchedule.put("location", new TableInfo.Column("location", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStaffSchedule.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStaffSchedule.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStaffSchedule = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStaffSchedule = new HashSet<TableInfo.Index>(2);
        _indicesStaffSchedule.add(new TableInfo.Index("index_staff_schedule_staffId", false, Arrays.asList("staffId"), Arrays.asList("ASC")));
        _indicesStaffSchedule.add(new TableInfo.Index("index_staff_schedule_scheduleDate", false, Arrays.asList("scheduleDate"), Arrays.asList("ASC")));
        final TableInfo _infoStaffSchedule = new TableInfo("staff_schedule", _columnsStaffSchedule, _foreignKeysStaffSchedule, _indicesStaffSchedule);
        final TableInfo _existingStaffSchedule = TableInfo.read(db, "staff_schedule");
        if (!_infoStaffSchedule.equals(_existingStaffSchedule)) {
          return new RoomOpenHelper.ValidationResult(false, "staff_schedule(com.elderlycare.app.data.community.StaffScheduleRecord).\n"
                  + " Expected:\n" + _infoStaffSchedule + "\n"
                  + " Found:\n" + _existingStaffSchedule);
        }
        final HashMap<String, TableInfo.Column> _columnsServiceRecord = new HashMap<String, TableInfo.Column>(8);
        _columnsServiceRecord.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceRecord.put("staffId", new TableInfo.Column("staffId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceRecord.put("elderlyId", new TableInfo.Column("elderlyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceRecord.put("elderlyName", new TableInfo.Column("elderlyName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceRecord.put("serviceType", new TableInfo.Column("serviceType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceRecord.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceRecord.put("durationMinutes", new TableInfo.Column("durationMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceRecord.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServiceRecord = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServiceRecord = new HashSet<TableInfo.Index>(3);
        _indicesServiceRecord.add(new TableInfo.Index("index_service_record_staffId", false, Arrays.asList("staffId"), Arrays.asList("ASC")));
        _indicesServiceRecord.add(new TableInfo.Index("index_service_record_elderlyId", false, Arrays.asList("elderlyId"), Arrays.asList("ASC")));
        _indicesServiceRecord.add(new TableInfo.Index("index_service_record_createdAt", false, Arrays.asList("createdAt"), Arrays.asList("ASC")));
        final TableInfo _infoServiceRecord = new TableInfo("service_record", _columnsServiceRecord, _foreignKeysServiceRecord, _indicesServiceRecord);
        final TableInfo _existingServiceRecord = TableInfo.read(db, "service_record");
        if (!_infoServiceRecord.equals(_existingServiceRecord)) {
          return new RoomOpenHelper.ValidationResult(false, "service_record(com.elderlycare.app.data.community.ServiceRecord).\n"
                  + " Expected:\n" + _infoServiceRecord + "\n"
                  + " Found:\n" + _existingServiceRecord);
        }
        final HashMap<String, TableInfo.Column> _columnsTodoItem = new HashMap<String, TableInfo.Column>(11);
        _columnsTodoItem.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("staffId", new TableInfo.Column("staffId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("elderlyId", new TableInfo.Column("elderlyId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("elderlyName", new TableInfo.Column("elderlyName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("todoType", new TableInfo.Column("todoType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("priority", new TableInfo.Column("priority", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTodoItem.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTodoItem = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTodoItem = new HashSet<TableInfo.Index>(3);
        _indicesTodoItem.add(new TableInfo.Index("index_todo_item_staffId", false, Arrays.asList("staffId"), Arrays.asList("ASC")));
        _indicesTodoItem.add(new TableInfo.Index("index_todo_item_elderlyId", false, Arrays.asList("elderlyId"), Arrays.asList("ASC")));
        _indicesTodoItem.add(new TableInfo.Index("index_todo_item_status", false, Arrays.asList("status"), Arrays.asList("ASC")));
        final TableInfo _infoTodoItem = new TableInfo("todo_item", _columnsTodoItem, _foreignKeysTodoItem, _indicesTodoItem);
        final TableInfo _existingTodoItem = TableInfo.read(db, "todo_item");
        if (!_infoTodoItem.equals(_existingTodoItem)) {
          return new RoomOpenHelper.ValidationResult(false, "todo_item(com.elderlycare.app.data.community.TodoItem).\n"
                  + " Expected:\n" + _infoTodoItem + "\n"
                  + " Found:\n" + _existingTodoItem);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "21daeb030cc244bd4c7140b47679ee7c", "833d08715d078fb24a861c54b7034d94");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "message","remind_plan","medical_follow_up_record","health_advice","community_follow_up","staff_schedule","service_record","todo_item");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `message`");
      _db.execSQL("DELETE FROM `remind_plan`");
      _db.execSQL("DELETE FROM `medical_follow_up_record`");
      _db.execSQL("DELETE FROM `health_advice`");
      _db.execSQL("DELETE FROM `community_follow_up`");
      _db.execSQL("DELETE FROM `staff_schedule`");
      _db.execSQL("DELETE FROM `service_record`");
      _db.execSQL("DELETE FROM `todo_item`");
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
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RemindPlanDao.class, RemindPlanDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MedicalFollowUpDao.class, MedicalFollowUpDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HealthAdviceDao.class, HealthAdviceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CommunityDao.class, CommunityDao_Impl.getRequiredConverters());
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
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }

  @Override
  public RemindPlanDao remindPlanDao() {
    if (_remindPlanDao != null) {
      return _remindPlanDao;
    } else {
      synchronized(this) {
        if(_remindPlanDao == null) {
          _remindPlanDao = new RemindPlanDao_Impl(this);
        }
        return _remindPlanDao;
      }
    }
  }

  @Override
  public MedicalFollowUpDao medicalFollowUpDao() {
    if (_medicalFollowUpDao != null) {
      return _medicalFollowUpDao;
    } else {
      synchronized(this) {
        if(_medicalFollowUpDao == null) {
          _medicalFollowUpDao = new MedicalFollowUpDao_Impl(this);
        }
        return _medicalFollowUpDao;
      }
    }
  }

  @Override
  public HealthAdviceDao healthAdviceDao() {
    if (_healthAdviceDao != null) {
      return _healthAdviceDao;
    } else {
      synchronized(this) {
        if(_healthAdviceDao == null) {
          _healthAdviceDao = new HealthAdviceDao_Impl(this);
        }
        return _healthAdviceDao;
      }
    }
  }

  @Override
  public CommunityDao communityDao() {
    if (_communityDao != null) {
      return _communityDao;
    } else {
      synchronized(this) {
        if(_communityDao == null) {
          _communityDao = new CommunityDao_Impl(this);
        }
        return _communityDao;
      }
    }
  }
}
