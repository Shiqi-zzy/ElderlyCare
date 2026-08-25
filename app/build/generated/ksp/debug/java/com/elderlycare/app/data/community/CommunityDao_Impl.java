package com.elderlycare.app.data.community;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
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
import java.lang.Integer;
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
public final class CommunityDao_Impl implements CommunityDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CommunityFollowUpRecord> __insertionAdapterOfCommunityFollowUpRecord;

  private final EntityInsertionAdapter<StaffScheduleRecord> __insertionAdapterOfStaffScheduleRecord;

  private final EntityInsertionAdapter<ServiceRecord> __insertionAdapterOfServiceRecord;

  private final EntityInsertionAdapter<TodoItem> __insertionAdapterOfTodoItem;

  private final EntityDeletionOrUpdateAdapter<CommunityFollowUpRecord> __updateAdapterOfCommunityFollowUpRecord;

  private final EntityDeletionOrUpdateAdapter<StaffScheduleRecord> __updateAdapterOfStaffScheduleRecord;

  private final EntityDeletionOrUpdateAdapter<TodoItem> __updateAdapterOfTodoItem;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFollowUpStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateScheduleStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTodoStatus;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTodo;

  public CommunityDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCommunityFollowUpRecord = new EntityInsertionAdapter<CommunityFollowUpRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `community_follow_up` (`id`,`elderlyId`,`elderlyName`,`staffId`,`followUpType`,`scheduledTime`,`content`,`status`,`createdAt`,`completedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CommunityFollowUpRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getElderlyId());
        statement.bindString(3, entity.getElderlyName());
        statement.bindString(4, entity.getStaffId());
        statement.bindString(5, entity.getFollowUpType());
        statement.bindLong(6, entity.getScheduledTime());
        statement.bindString(7, entity.getContent());
        statement.bindString(8, entity.getStatus());
        statement.bindLong(9, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getCompletedAt());
        }
      }
    };
    this.__insertionAdapterOfStaffScheduleRecord = new EntityInsertionAdapter<StaffScheduleRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `staff_schedule` (`id`,`staffId`,`title`,`scheduleDate`,`startTime`,`endTime`,`location`,`status`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StaffScheduleRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getStaffId());
        statement.bindString(3, entity.getTitle());
        statement.bindLong(4, entity.getScheduleDate());
        statement.bindString(5, entity.getStartTime());
        statement.bindString(6, entity.getEndTime());
        statement.bindString(7, entity.getLocation());
        statement.bindString(8, entity.getStatus());
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfServiceRecord = new EntityInsertionAdapter<ServiceRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `service_record` (`id`,`staffId`,`elderlyId`,`elderlyName`,`serviceType`,`content`,`durationMinutes`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServiceRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getStaffId());
        statement.bindString(3, entity.getElderlyId());
        statement.bindString(4, entity.getElderlyName());
        statement.bindString(5, entity.getServiceType());
        statement.bindString(6, entity.getContent());
        statement.bindLong(7, entity.getDurationMinutes());
        statement.bindLong(8, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfTodoItem = new EntityInsertionAdapter<TodoItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `todo_item` (`id`,`staffId`,`elderlyId`,`elderlyName`,`todoType`,`title`,`content`,`priority`,`status`,`createdAt`,`completedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TodoItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getStaffId());
        statement.bindString(3, entity.getElderlyId());
        statement.bindString(4, entity.getElderlyName());
        statement.bindString(5, entity.getTodoType());
        statement.bindString(6, entity.getTitle());
        statement.bindString(7, entity.getContent());
        statement.bindString(8, entity.getPriority());
        statement.bindString(9, entity.getStatus());
        statement.bindLong(10, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getCompletedAt());
        }
      }
    };
    this.__updateAdapterOfCommunityFollowUpRecord = new EntityDeletionOrUpdateAdapter<CommunityFollowUpRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `community_follow_up` SET `id` = ?,`elderlyId` = ?,`elderlyName` = ?,`staffId` = ?,`followUpType` = ?,`scheduledTime` = ?,`content` = ?,`status` = ?,`createdAt` = ?,`completedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CommunityFollowUpRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getElderlyId());
        statement.bindString(3, entity.getElderlyName());
        statement.bindString(4, entity.getStaffId());
        statement.bindString(5, entity.getFollowUpType());
        statement.bindLong(6, entity.getScheduledTime());
        statement.bindString(7, entity.getContent());
        statement.bindString(8, entity.getStatus());
        statement.bindLong(9, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getCompletedAt());
        }
        statement.bindLong(11, entity.getId());
      }
    };
    this.__updateAdapterOfStaffScheduleRecord = new EntityDeletionOrUpdateAdapter<StaffScheduleRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `staff_schedule` SET `id` = ?,`staffId` = ?,`title` = ?,`scheduleDate` = ?,`startTime` = ?,`endTime` = ?,`location` = ?,`status` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StaffScheduleRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getStaffId());
        statement.bindString(3, entity.getTitle());
        statement.bindLong(4, entity.getScheduleDate());
        statement.bindString(5, entity.getStartTime());
        statement.bindString(6, entity.getEndTime());
        statement.bindString(7, entity.getLocation());
        statement.bindString(8, entity.getStatus());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getId());
      }
    };
    this.__updateAdapterOfTodoItem = new EntityDeletionOrUpdateAdapter<TodoItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `todo_item` SET `id` = ?,`staffId` = ?,`elderlyId` = ?,`elderlyName` = ?,`todoType` = ?,`title` = ?,`content` = ?,`priority` = ?,`status` = ?,`createdAt` = ?,`completedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TodoItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getStaffId());
        statement.bindString(3, entity.getElderlyId());
        statement.bindString(4, entity.getElderlyName());
        statement.bindString(5, entity.getTodoType());
        statement.bindString(6, entity.getTitle());
        statement.bindString(7, entity.getContent());
        statement.bindString(8, entity.getPriority());
        statement.bindString(9, entity.getStatus());
        statement.bindLong(10, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getCompletedAt());
        }
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateFollowUpStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE community_follow_up SET status = ?, completedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateScheduleStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE staff_schedule SET status = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateTodoStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE todo_item SET status = ?, completedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteTodo = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM todo_item WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertFollowUp(final CommunityFollowUpRecord record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCommunityFollowUpRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSchedule(final StaffScheduleRecord record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfStaffScheduleRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertServiceRecord(final ServiceRecord record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfServiceRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTodo(final TodoItem item, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTodoItem.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFollowUp(final CommunityFollowUpRecord record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCommunityFollowUpRecord.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSchedule(final StaffScheduleRecord record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfStaffScheduleRecord.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTodo(final TodoItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTodoItem.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFollowUpStatus(final long id, final String status, final Long completedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFollowUpStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        if (completedAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, completedAt);
        }
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
          __preparedStmtOfUpdateFollowUpStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateScheduleStatus(final long id, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateScheduleStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
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
          __preparedStmtOfUpdateScheduleStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTodoStatus(final long id, final String status, final Long completedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTodoStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        if (completedAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, completedAt);
        }
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
          __preparedStmtOfUpdateTodoStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTodo(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTodo.acquire();
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
          __preparedStmtOfDeleteTodo.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CommunityFollowUpRecord>> observeFollowUps(final String staffId) {
    final String _sql = "SELECT * FROM community_follow_up WHERE staffId = ? ORDER BY scheduledTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, staffId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"community_follow_up"}, new Callable<List<CommunityFollowUpRecord>>() {
      @Override
      @NonNull
      public List<CommunityFollowUpRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfElderlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyName");
          final int _cursorIndexOfStaffId = CursorUtil.getColumnIndexOrThrow(_cursor, "staffId");
          final int _cursorIndexOfFollowUpType = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpType");
          final int _cursorIndexOfScheduledTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledTime");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<CommunityFollowUpRecord> _result = new ArrayList<CommunityFollowUpRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CommunityFollowUpRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpElderlyName;
            _tmpElderlyName = _cursor.getString(_cursorIndexOfElderlyName);
            final String _tmpStaffId;
            _tmpStaffId = _cursor.getString(_cursorIndexOfStaffId);
            final String _tmpFollowUpType;
            _tmpFollowUpType = _cursor.getString(_cursorIndexOfFollowUpType);
            final long _tmpScheduledTime;
            _tmpScheduledTime = _cursor.getLong(_cursorIndexOfScheduledTime);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new CommunityFollowUpRecord(_tmpId,_tmpElderlyId,_tmpElderlyName,_tmpStaffId,_tmpFollowUpType,_tmpScheduledTime,_tmpContent,_tmpStatus,_tmpCreatedAt,_tmpCompletedAt);
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
  public Object getFollowUpsByStatus(final String staffId, final String status,
      final Continuation<? super List<CommunityFollowUpRecord>> $completion) {
    final String _sql = "SELECT * FROM community_follow_up WHERE staffId = ? AND status = ? ORDER BY scheduledTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, staffId);
    _argIndex = 2;
    _statement.bindString(_argIndex, status);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CommunityFollowUpRecord>>() {
      @Override
      @NonNull
      public List<CommunityFollowUpRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfElderlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyName");
          final int _cursorIndexOfStaffId = CursorUtil.getColumnIndexOrThrow(_cursor, "staffId");
          final int _cursorIndexOfFollowUpType = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpType");
          final int _cursorIndexOfScheduledTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledTime");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<CommunityFollowUpRecord> _result = new ArrayList<CommunityFollowUpRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CommunityFollowUpRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpElderlyName;
            _tmpElderlyName = _cursor.getString(_cursorIndexOfElderlyName);
            final String _tmpStaffId;
            _tmpStaffId = _cursor.getString(_cursorIndexOfStaffId);
            final String _tmpFollowUpType;
            _tmpFollowUpType = _cursor.getString(_cursorIndexOfFollowUpType);
            final long _tmpScheduledTime;
            _tmpScheduledTime = _cursor.getLong(_cursorIndexOfScheduledTime);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new CommunityFollowUpRecord(_tmpId,_tmpElderlyId,_tmpElderlyName,_tmpStaffId,_tmpFollowUpType,_tmpScheduledTime,_tmpContent,_tmpStatus,_tmpCreatedAt,_tmpCompletedAt);
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
  public Object getFollowUpsByElderlyAndType(final String elderlyId, final String followUpType,
      final String status, final Continuation<? super List<CommunityFollowUpRecord>> $completion) {
    final String _sql = "SELECT * FROM community_follow_up WHERE elderlyId = ? AND followUpType = ? AND status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    _argIndex = 2;
    _statement.bindString(_argIndex, followUpType);
    _argIndex = 3;
    _statement.bindString(_argIndex, status);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CommunityFollowUpRecord>>() {
      @Override
      @NonNull
      public List<CommunityFollowUpRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfElderlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyName");
          final int _cursorIndexOfStaffId = CursorUtil.getColumnIndexOrThrow(_cursor, "staffId");
          final int _cursorIndexOfFollowUpType = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpType");
          final int _cursorIndexOfScheduledTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledTime");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<CommunityFollowUpRecord> _result = new ArrayList<CommunityFollowUpRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CommunityFollowUpRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpElderlyName;
            _tmpElderlyName = _cursor.getString(_cursorIndexOfElderlyName);
            final String _tmpStaffId;
            _tmpStaffId = _cursor.getString(_cursorIndexOfStaffId);
            final String _tmpFollowUpType;
            _tmpFollowUpType = _cursor.getString(_cursorIndexOfFollowUpType);
            final long _tmpScheduledTime;
            _tmpScheduledTime = _cursor.getLong(_cursorIndexOfScheduledTime);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new CommunityFollowUpRecord(_tmpId,_tmpElderlyId,_tmpElderlyName,_tmpStaffId,_tmpFollowUpType,_tmpScheduledTime,_tmpContent,_tmpStatus,_tmpCreatedAt,_tmpCompletedAt);
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
  public Flow<List<StaffScheduleRecord>> observeSchedules(final String staffId) {
    final String _sql = "SELECT * FROM staff_schedule WHERE staffId = ? ORDER BY scheduleDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, staffId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"staff_schedule"}, new Callable<List<StaffScheduleRecord>>() {
      @Override
      @NonNull
      public List<StaffScheduleRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStaffId = CursorUtil.getColumnIndexOrThrow(_cursor, "staffId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfScheduleDate = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleDate");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<StaffScheduleRecord> _result = new ArrayList<StaffScheduleRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StaffScheduleRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpStaffId;
            _tmpStaffId = _cursor.getString(_cursorIndexOfStaffId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpScheduleDate;
            _tmpScheduleDate = _cursor.getLong(_cursorIndexOfScheduleDate);
            final String _tmpStartTime;
            _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime);
            final String _tmpEndTime;
            _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new StaffScheduleRecord(_tmpId,_tmpStaffId,_tmpTitle,_tmpScheduleDate,_tmpStartTime,_tmpEndTime,_tmpLocation,_tmpStatus,_tmpCreatedAt);
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
  public Flow<List<ServiceRecord>> observeServiceRecords(final String staffId) {
    final String _sql = "SELECT * FROM service_record WHERE staffId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, staffId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"service_record"}, new Callable<List<ServiceRecord>>() {
      @Override
      @NonNull
      public List<ServiceRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStaffId = CursorUtil.getColumnIndexOrThrow(_cursor, "staffId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfElderlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyName");
          final int _cursorIndexOfServiceType = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceType");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<ServiceRecord> _result = new ArrayList<ServiceRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServiceRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpStaffId;
            _tmpStaffId = _cursor.getString(_cursorIndexOfStaffId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpElderlyName;
            _tmpElderlyName = _cursor.getString(_cursorIndexOfElderlyName);
            final String _tmpServiceType;
            _tmpServiceType = _cursor.getString(_cursorIndexOfServiceType);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final int _tmpDurationMinutes;
            _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ServiceRecord(_tmpId,_tmpStaffId,_tmpElderlyId,_tmpElderlyName,_tmpServiceType,_tmpContent,_tmpDurationMinutes,_tmpCreatedAt);
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
  public Object countServiceRecords(final String staffId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM service_record WHERE staffId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, staffId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Flow<List<TodoItem>> observeTodosByStatus(final String staffId, final String status) {
    final String _sql = "SELECT * FROM todo_item WHERE staffId = ? AND status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, staffId);
    _argIndex = 2;
    _statement.bindString(_argIndex, status);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"todo_item"}, new Callable<List<TodoItem>>() {
      @Override
      @NonNull
      public List<TodoItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStaffId = CursorUtil.getColumnIndexOrThrow(_cursor, "staffId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfElderlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyName");
          final int _cursorIndexOfTodoType = CursorUtil.getColumnIndexOrThrow(_cursor, "todoType");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<TodoItem> _result = new ArrayList<TodoItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TodoItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpStaffId;
            _tmpStaffId = _cursor.getString(_cursorIndexOfStaffId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpElderlyName;
            _tmpElderlyName = _cursor.getString(_cursorIndexOfElderlyName);
            final String _tmpTodoType;
            _tmpTodoType = _cursor.getString(_cursorIndexOfTodoType);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new TodoItem(_tmpId,_tmpStaffId,_tmpElderlyId,_tmpElderlyName,_tmpTodoType,_tmpTitle,_tmpContent,_tmpPriority,_tmpStatus,_tmpCreatedAt,_tmpCompletedAt);
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
  public Flow<List<TodoItem>> observeAllTodos(final String staffId) {
    final String _sql = "SELECT * FROM todo_item WHERE staffId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, staffId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"todo_item"}, new Callable<List<TodoItem>>() {
      @Override
      @NonNull
      public List<TodoItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStaffId = CursorUtil.getColumnIndexOrThrow(_cursor, "staffId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfElderlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyName");
          final int _cursorIndexOfTodoType = CursorUtil.getColumnIndexOrThrow(_cursor, "todoType");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<TodoItem> _result = new ArrayList<TodoItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TodoItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpStaffId;
            _tmpStaffId = _cursor.getString(_cursorIndexOfStaffId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpElderlyName;
            _tmpElderlyName = _cursor.getString(_cursorIndexOfElderlyName);
            final String _tmpTodoType;
            _tmpTodoType = _cursor.getString(_cursorIndexOfTodoType);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new TodoItem(_tmpId,_tmpStaffId,_tmpElderlyId,_tmpElderlyName,_tmpTodoType,_tmpTitle,_tmpContent,_tmpPriority,_tmpStatus,_tmpCreatedAt,_tmpCompletedAt);
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
  public Object getTodosByElderlyAndType(final String elderlyId, final String todoType,
      final String status, final Continuation<? super List<TodoItem>> $completion) {
    final String _sql = "SELECT * FROM todo_item WHERE elderlyId = ? AND todoType = ? AND status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    _argIndex = 2;
    _statement.bindString(_argIndex, todoType);
    _argIndex = 3;
    _statement.bindString(_argIndex, status);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TodoItem>>() {
      @Override
      @NonNull
      public List<TodoItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStaffId = CursorUtil.getColumnIndexOrThrow(_cursor, "staffId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfElderlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyName");
          final int _cursorIndexOfTodoType = CursorUtil.getColumnIndexOrThrow(_cursor, "todoType");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<TodoItem> _result = new ArrayList<TodoItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TodoItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpStaffId;
            _tmpStaffId = _cursor.getString(_cursorIndexOfStaffId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpElderlyName;
            _tmpElderlyName = _cursor.getString(_cursorIndexOfElderlyName);
            final String _tmpTodoType;
            _tmpTodoType = _cursor.getString(_cursorIndexOfTodoType);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new TodoItem(_tmpId,_tmpStaffId,_tmpElderlyId,_tmpElderlyName,_tmpTodoType,_tmpTitle,_tmpContent,_tmpPriority,_tmpStatus,_tmpCreatedAt,_tmpCompletedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
