package com.elderlycare.app.data.reminder;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RemindPlanDao_Impl implements RemindPlanDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RemindPlanEntity> __insertionAdapterOfRemindPlanEntity;

  private final EntityDeletionOrUpdateAdapter<RemindPlanEntity> __deletionAdapterOfRemindPlanEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFromDevice;

  private final SharedSQLiteStatement __preparedStmtOfMarkExecuted;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByDeviceSerial;

  private final SharedSQLiteStatement __preparedStmtOfUpdateConfirmStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateConfirmStatusAndClockId;

  public RemindPlanDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRemindPlanEntity = new EntityInsertionAdapter<RemindPlanEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `remind_plan` (`id`,`clockId`,`tag`,`content`,`timeHour`,`timeMin`,`repeatType`,`weekdays`,`year`,`month`,`day`,`enabled`,`executed`,`deviceSerial`,`createTime`,`source`,`confirmStatus`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RemindPlanEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getClockId());
        statement.bindString(3, entity.getTag());
        statement.bindString(4, entity.getContent());
        statement.bindLong(5, entity.getTimeHour());
        statement.bindLong(6, entity.getTimeMin());
        statement.bindLong(7, entity.getRepeatType());
        statement.bindString(8, entity.getWeekdays());
        statement.bindLong(9, entity.getYear());
        statement.bindLong(10, entity.getMonth());
        statement.bindLong(11, entity.getDay());
        statement.bindLong(12, entity.getEnabled());
        statement.bindLong(13, entity.getExecuted());
        statement.bindString(14, entity.getDeviceSerial());
        statement.bindLong(15, entity.getCreateTime());
        statement.bindLong(16, entity.getSource());
        statement.bindLong(17, entity.getConfirmStatus());
      }
    };
    this.__deletionAdapterOfRemindPlanEntity = new EntityDeletionOrUpdateAdapter<RemindPlanEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `remind_plan` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RemindPlanEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateFromDevice = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE remind_plan SET tag = ?, content = ?, timeHour = ?, timeMin = ?, repeatType = ?, weekdays = ?, year = ?, month = ?, day = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkExecuted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE remind_plan SET executed = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByDeviceSerial = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM remind_plan WHERE deviceSerial = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateConfirmStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE remind_plan SET confirmStatus = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateConfirmStatusAndClockId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE remind_plan SET confirmStatus = ?, clockId = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<RemindPlanEntity> plans,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRemindPlanEntity.insert(plans);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final RemindPlanEntity plan, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfRemindPlanEntity.insertAndReturnId(plan);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final RemindPlanEntity plan, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRemindPlanEntity.handle(plan);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFromDevice(final long id, final String tag, final String content,
      final int timeHour, final int timeMin, final int repeatType, final String weekdays,
      final int year, final int month, final int day,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFromDevice.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, tag);
        _argIndex = 2;
        _stmt.bindString(_argIndex, content);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, timeHour);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, timeMin);
        _argIndex = 5;
        _stmt.bindLong(_argIndex, repeatType);
        _argIndex = 6;
        _stmt.bindString(_argIndex, weekdays);
        _argIndex = 7;
        _stmt.bindLong(_argIndex, year);
        _argIndex = 8;
        _stmt.bindLong(_argIndex, month);
        _argIndex = 9;
        _stmt.bindLong(_argIndex, day);
        _argIndex = 10;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateFromDevice.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markExecuted(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkExecuted.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkExecuted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByDeviceSerial(final String deviceSerial,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByDeviceSerial.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, deviceSerial);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByDeviceSerial.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateConfirmStatus(final long id, final int status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateConfirmStatus.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateConfirmStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateConfirmStatusAndClockId(final long id, final int status, final String clockId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateConfirmStatusAndClockId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, status);
        _argIndex = 2;
        _stmt.bindString(_argIndex, clockId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateConfirmStatusAndClockId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RemindPlanEntity>> observeByDeviceSerial(final String deviceSerial) {
    final String _sql = "SELECT * FROM remind_plan WHERE deviceSerial = ? ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceSerial);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"remind_plan"}, new Callable<List<RemindPlanEntity>>() {
      @Override
      @NonNull
      public List<RemindPlanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClockId = CursorUtil.getColumnIndexOrThrow(_cursor, "clockId");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimeHour = CursorUtil.getColumnIndexOrThrow(_cursor, "timeHour");
          final int _cursorIndexOfTimeMin = CursorUtil.getColumnIndexOrThrow(_cursor, "timeMin");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "day");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "executed");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfConfirmStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "confirmStatus");
          final List<RemindPlanEntity> _result = new ArrayList<RemindPlanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RemindPlanEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClockId;
            _tmpClockId = _cursor.getString(_cursorIndexOfClockId);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpTimeHour;
            _tmpTimeHour = _cursor.getInt(_cursorIndexOfTimeHour);
            final int _tmpTimeMin;
            _tmpTimeMin = _cursor.getInt(_cursorIndexOfTimeMin);
            final int _tmpRepeatType;
            _tmpRepeatType = _cursor.getInt(_cursorIndexOfRepeatType);
            final String _tmpWeekdays;
            _tmpWeekdays = _cursor.getString(_cursorIndexOfWeekdays);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpDay;
            _tmpDay = _cursor.getInt(_cursorIndexOfDay);
            final int _tmpEnabled;
            _tmpEnabled = _cursor.getInt(_cursorIndexOfEnabled);
            final int _tmpExecuted;
            _tmpExecuted = _cursor.getInt(_cursorIndexOfExecuted);
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final int _tmpConfirmStatus;
            _tmpConfirmStatus = _cursor.getInt(_cursorIndexOfConfirmStatus);
            _item = new RemindPlanEntity(_tmpId,_tmpClockId,_tmpTag,_tmpContent,_tmpTimeHour,_tmpTimeMin,_tmpRepeatType,_tmpWeekdays,_tmpYear,_tmpMonth,_tmpDay,_tmpEnabled,_tmpExecuted,_tmpDeviceSerial,_tmpCreateTime,_tmpSource,_tmpConfirmStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<RemindPlanEntity> observeById(final long id) {
    final String _sql = "SELECT * FROM remind_plan WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"remind_plan"}, new Callable<RemindPlanEntity>() {
      @Override
      @Nullable
      public RemindPlanEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClockId = CursorUtil.getColumnIndexOrThrow(_cursor, "clockId");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimeHour = CursorUtil.getColumnIndexOrThrow(_cursor, "timeHour");
          final int _cursorIndexOfTimeMin = CursorUtil.getColumnIndexOrThrow(_cursor, "timeMin");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "day");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "executed");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfConfirmStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "confirmStatus");
          final RemindPlanEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClockId;
            _tmpClockId = _cursor.getString(_cursorIndexOfClockId);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpTimeHour;
            _tmpTimeHour = _cursor.getInt(_cursorIndexOfTimeHour);
            final int _tmpTimeMin;
            _tmpTimeMin = _cursor.getInt(_cursorIndexOfTimeMin);
            final int _tmpRepeatType;
            _tmpRepeatType = _cursor.getInt(_cursorIndexOfRepeatType);
            final String _tmpWeekdays;
            _tmpWeekdays = _cursor.getString(_cursorIndexOfWeekdays);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpDay;
            _tmpDay = _cursor.getInt(_cursorIndexOfDay);
            final int _tmpEnabled;
            _tmpEnabled = _cursor.getInt(_cursorIndexOfEnabled);
            final int _tmpExecuted;
            _tmpExecuted = _cursor.getInt(_cursorIndexOfExecuted);
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final int _tmpConfirmStatus;
            _tmpConfirmStatus = _cursor.getInt(_cursorIndexOfConfirmStatus);
            _result = new RemindPlanEntity(_tmpId,_tmpClockId,_tmpTag,_tmpContent,_tmpTimeHour,_tmpTimeMin,_tmpRepeatType,_tmpWeekdays,_tmpYear,_tmpMonth,_tmpDay,_tmpEnabled,_tmpExecuted,_tmpDeviceSerial,_tmpCreateTime,_tmpSource,_tmpConfirmStatus);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllByDeviceSerial(final String deviceSerial,
      final Continuation<? super List<RemindPlanEntity>> $completion) {
    final String _sql = "SELECT * FROM remind_plan WHERE deviceSerial = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceSerial);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RemindPlanEntity>>() {
      @Override
      @NonNull
      public List<RemindPlanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClockId = CursorUtil.getColumnIndexOrThrow(_cursor, "clockId");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimeHour = CursorUtil.getColumnIndexOrThrow(_cursor, "timeHour");
          final int _cursorIndexOfTimeMin = CursorUtil.getColumnIndexOrThrow(_cursor, "timeMin");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "day");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "executed");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfConfirmStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "confirmStatus");
          final List<RemindPlanEntity> _result = new ArrayList<RemindPlanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RemindPlanEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClockId;
            _tmpClockId = _cursor.getString(_cursorIndexOfClockId);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpTimeHour;
            _tmpTimeHour = _cursor.getInt(_cursorIndexOfTimeHour);
            final int _tmpTimeMin;
            _tmpTimeMin = _cursor.getInt(_cursorIndexOfTimeMin);
            final int _tmpRepeatType;
            _tmpRepeatType = _cursor.getInt(_cursorIndexOfRepeatType);
            final String _tmpWeekdays;
            _tmpWeekdays = _cursor.getString(_cursorIndexOfWeekdays);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpDay;
            _tmpDay = _cursor.getInt(_cursorIndexOfDay);
            final int _tmpEnabled;
            _tmpEnabled = _cursor.getInt(_cursorIndexOfEnabled);
            final int _tmpExecuted;
            _tmpExecuted = _cursor.getInt(_cursorIndexOfExecuted);
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final int _tmpConfirmStatus;
            _tmpConfirmStatus = _cursor.getInt(_cursorIndexOfConfirmStatus);
            _item = new RemindPlanEntity(_tmpId,_tmpClockId,_tmpTag,_tmpContent,_tmpTimeHour,_tmpTimeMin,_tmpRepeatType,_tmpWeekdays,_tmpYear,_tmpMonth,_tmpDay,_tmpEnabled,_tmpExecuted,_tmpDeviceSerial,_tmpCreateTime,_tmpSource,_tmpConfirmStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByClockId(final String clockId,
      final Continuation<? super RemindPlanEntity> $completion) {
    final String _sql = "SELECT * FROM remind_plan WHERE clockId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, clockId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RemindPlanEntity>() {
      @Override
      @Nullable
      public RemindPlanEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClockId = CursorUtil.getColumnIndexOrThrow(_cursor, "clockId");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimeHour = CursorUtil.getColumnIndexOrThrow(_cursor, "timeHour");
          final int _cursorIndexOfTimeMin = CursorUtil.getColumnIndexOrThrow(_cursor, "timeMin");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "day");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "executed");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfConfirmStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "confirmStatus");
          final RemindPlanEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClockId;
            _tmpClockId = _cursor.getString(_cursorIndexOfClockId);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpTimeHour;
            _tmpTimeHour = _cursor.getInt(_cursorIndexOfTimeHour);
            final int _tmpTimeMin;
            _tmpTimeMin = _cursor.getInt(_cursorIndexOfTimeMin);
            final int _tmpRepeatType;
            _tmpRepeatType = _cursor.getInt(_cursorIndexOfRepeatType);
            final String _tmpWeekdays;
            _tmpWeekdays = _cursor.getString(_cursorIndexOfWeekdays);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpDay;
            _tmpDay = _cursor.getInt(_cursorIndexOfDay);
            final int _tmpEnabled;
            _tmpEnabled = _cursor.getInt(_cursorIndexOfEnabled);
            final int _tmpExecuted;
            _tmpExecuted = _cursor.getInt(_cursorIndexOfExecuted);
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final int _tmpConfirmStatus;
            _tmpConfirmStatus = _cursor.getInt(_cursorIndexOfConfirmStatus);
            _result = new RemindPlanEntity(_tmpId,_tmpClockId,_tmpTag,_tmpContent,_tmpTimeHour,_tmpTimeMin,_tmpRepeatType,_tmpWeekdays,_tmpYear,_tmpMonth,_tmpDay,_tmpEnabled,_tmpExecuted,_tmpDeviceSerial,_tmpCreateTime,_tmpSource,_tmpConfirmStatus);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RemindPlanEntity>> observeHospitalPlans() {
    final String _sql = "SELECT * FROM remind_plan WHERE source != 0 ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"remind_plan"}, new Callable<List<RemindPlanEntity>>() {
      @Override
      @NonNull
      public List<RemindPlanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClockId = CursorUtil.getColumnIndexOrThrow(_cursor, "clockId");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimeHour = CursorUtil.getColumnIndexOrThrow(_cursor, "timeHour");
          final int _cursorIndexOfTimeMin = CursorUtil.getColumnIndexOrThrow(_cursor, "timeMin");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "day");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "executed");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfConfirmStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "confirmStatus");
          final List<RemindPlanEntity> _result = new ArrayList<RemindPlanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RemindPlanEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClockId;
            _tmpClockId = _cursor.getString(_cursorIndexOfClockId);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpTimeHour;
            _tmpTimeHour = _cursor.getInt(_cursorIndexOfTimeHour);
            final int _tmpTimeMin;
            _tmpTimeMin = _cursor.getInt(_cursorIndexOfTimeMin);
            final int _tmpRepeatType;
            _tmpRepeatType = _cursor.getInt(_cursorIndexOfRepeatType);
            final String _tmpWeekdays;
            _tmpWeekdays = _cursor.getString(_cursorIndexOfWeekdays);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpDay;
            _tmpDay = _cursor.getInt(_cursorIndexOfDay);
            final int _tmpEnabled;
            _tmpEnabled = _cursor.getInt(_cursorIndexOfEnabled);
            final int _tmpExecuted;
            _tmpExecuted = _cursor.getInt(_cursorIndexOfExecuted);
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final int _tmpConfirmStatus;
            _tmpConfirmStatus = _cursor.getInt(_cursorIndexOfConfirmStatus);
            _item = new RemindPlanEntity(_tmpId,_tmpClockId,_tmpTag,_tmpContent,_tmpTimeHour,_tmpTimeMin,_tmpRepeatType,_tmpWeekdays,_tmpYear,_tmpMonth,_tmpDay,_tmpEnabled,_tmpExecuted,_tmpDeviceSerial,_tmpCreateTime,_tmpSource,_tmpConfirmStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllHospitalPlans(
      final Continuation<? super List<RemindPlanEntity>> $completion) {
    final String _sql = "SELECT * FROM remind_plan WHERE source != 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RemindPlanEntity>>() {
      @Override
      @NonNull
      public List<RemindPlanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClockId = CursorUtil.getColumnIndexOrThrow(_cursor, "clockId");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimeHour = CursorUtil.getColumnIndexOrThrow(_cursor, "timeHour");
          final int _cursorIndexOfTimeMin = CursorUtil.getColumnIndexOrThrow(_cursor, "timeMin");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "day");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "executed");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfConfirmStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "confirmStatus");
          final List<RemindPlanEntity> _result = new ArrayList<RemindPlanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RemindPlanEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClockId;
            _tmpClockId = _cursor.getString(_cursorIndexOfClockId);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpTimeHour;
            _tmpTimeHour = _cursor.getInt(_cursorIndexOfTimeHour);
            final int _tmpTimeMin;
            _tmpTimeMin = _cursor.getInt(_cursorIndexOfTimeMin);
            final int _tmpRepeatType;
            _tmpRepeatType = _cursor.getInt(_cursorIndexOfRepeatType);
            final String _tmpWeekdays;
            _tmpWeekdays = _cursor.getString(_cursorIndexOfWeekdays);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpDay;
            _tmpDay = _cursor.getInt(_cursorIndexOfDay);
            final int _tmpEnabled;
            _tmpEnabled = _cursor.getInt(_cursorIndexOfEnabled);
            final int _tmpExecuted;
            _tmpExecuted = _cursor.getInt(_cursorIndexOfExecuted);
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final int _tmpConfirmStatus;
            _tmpConfirmStatus = _cursor.getInt(_cursorIndexOfConfirmStatus);
            _item = new RemindPlanEntity(_tmpId,_tmpClockId,_tmpTag,_tmpContent,_tmpTimeHour,_tmpTimeMin,_tmpRepeatType,_tmpWeekdays,_tmpYear,_tmpMonth,_tmpDay,_tmpEnabled,_tmpExecuted,_tmpDeviceSerial,_tmpCreateTime,_tmpSource,_tmpConfirmStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getExecutedHospitalDevicePlans(
      final Continuation<? super List<RemindPlanEntity>> $completion) {
    final String _sql = "SELECT * FROM remind_plan WHERE source = 2 AND executed = 1 AND clockId != ''";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RemindPlanEntity>>() {
      @Override
      @NonNull
      public List<RemindPlanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClockId = CursorUtil.getColumnIndexOrThrow(_cursor, "clockId");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimeHour = CursorUtil.getColumnIndexOrThrow(_cursor, "timeHour");
          final int _cursorIndexOfTimeMin = CursorUtil.getColumnIndexOrThrow(_cursor, "timeMin");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "day");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "executed");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfConfirmStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "confirmStatus");
          final List<RemindPlanEntity> _result = new ArrayList<RemindPlanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RemindPlanEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClockId;
            _tmpClockId = _cursor.getString(_cursorIndexOfClockId);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpTimeHour;
            _tmpTimeHour = _cursor.getInt(_cursorIndexOfTimeHour);
            final int _tmpTimeMin;
            _tmpTimeMin = _cursor.getInt(_cursorIndexOfTimeMin);
            final int _tmpRepeatType;
            _tmpRepeatType = _cursor.getInt(_cursorIndexOfRepeatType);
            final String _tmpWeekdays;
            _tmpWeekdays = _cursor.getString(_cursorIndexOfWeekdays);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpDay;
            _tmpDay = _cursor.getInt(_cursorIndexOfDay);
            final int _tmpEnabled;
            _tmpEnabled = _cursor.getInt(_cursorIndexOfEnabled);
            final int _tmpExecuted;
            _tmpExecuted = _cursor.getInt(_cursorIndexOfExecuted);
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final int _tmpConfirmStatus;
            _tmpConfirmStatus = _cursor.getInt(_cursorIndexOfConfirmStatus);
            _item = new RemindPlanEntity(_tmpId,_tmpClockId,_tmpTag,_tmpContent,_tmpTimeHour,_tmpTimeMin,_tmpRepeatType,_tmpWeekdays,_tmpYear,_tmpMonth,_tmpDay,_tmpEnabled,_tmpExecuted,_tmpDeviceSerial,_tmpCreateTime,_tmpSource,_tmpConfirmStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RemindPlanEntity>> observePendingConfirmPlans(final String deviceSerial) {
    final String _sql = "SELECT * FROM remind_plan WHERE deviceSerial = ? AND source = 2 AND confirmStatus = 1 ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceSerial);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"remind_plan"}, new Callable<List<RemindPlanEntity>>() {
      @Override
      @NonNull
      public List<RemindPlanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClockId = CursorUtil.getColumnIndexOrThrow(_cursor, "clockId");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimeHour = CursorUtil.getColumnIndexOrThrow(_cursor, "timeHour");
          final int _cursorIndexOfTimeMin = CursorUtil.getColumnIndexOrThrow(_cursor, "timeMin");
          final int _cursorIndexOfRepeatType = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "day");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfExecuted = CursorUtil.getColumnIndexOrThrow(_cursor, "executed");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfConfirmStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "confirmStatus");
          final List<RemindPlanEntity> _result = new ArrayList<RemindPlanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RemindPlanEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClockId;
            _tmpClockId = _cursor.getString(_cursorIndexOfClockId);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpTimeHour;
            _tmpTimeHour = _cursor.getInt(_cursorIndexOfTimeHour);
            final int _tmpTimeMin;
            _tmpTimeMin = _cursor.getInt(_cursorIndexOfTimeMin);
            final int _tmpRepeatType;
            _tmpRepeatType = _cursor.getInt(_cursorIndexOfRepeatType);
            final String _tmpWeekdays;
            _tmpWeekdays = _cursor.getString(_cursorIndexOfWeekdays);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpDay;
            _tmpDay = _cursor.getInt(_cursorIndexOfDay);
            final int _tmpEnabled;
            _tmpEnabled = _cursor.getInt(_cursorIndexOfEnabled);
            final int _tmpExecuted;
            _tmpExecuted = _cursor.getInt(_cursorIndexOfExecuted);
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final int _tmpConfirmStatus;
            _tmpConfirmStatus = _cursor.getInt(_cursorIndexOfConfirmStatus);
            _item = new RemindPlanEntity(_tmpId,_tmpClockId,_tmpTag,_tmpContent,_tmpTimeHour,_tmpTimeMin,_tmpRepeatType,_tmpWeekdays,_tmpYear,_tmpMonth,_tmpDay,_tmpEnabled,_tmpExecuted,_tmpDeviceSerial,_tmpCreateTime,_tmpSource,_tmpConfirmStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
