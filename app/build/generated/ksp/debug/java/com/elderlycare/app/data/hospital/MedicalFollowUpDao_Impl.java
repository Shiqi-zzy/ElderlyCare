package com.elderlycare.app.data.hospital;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
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
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MedicalFollowUpDao_Impl implements MedicalFollowUpDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MedicalFollowUpRecord> __insertionAdapterOfMedicalFollowUpRecord;

  public MedicalFollowUpDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMedicalFollowUpRecord = new EntityInsertionAdapter<MedicalFollowUpRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `medical_follow_up_record` (`id`,`elderlyId`,`followUpTime`,`content`,`status`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MedicalFollowUpRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getElderlyId());
        statement.bindLong(3, entity.getFollowUpTime());
        statement.bindString(4, entity.getContent());
        statement.bindString(5, entity.getStatus());
      }
    };
  }

  @Override
  public Object insert(final MedicalFollowUpRecord record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMedicalFollowUpRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MedicalFollowUpRecord>> observeByElderlyId(final String elderlyId) {
    final String _sql = "SELECT * FROM medical_follow_up_record WHERE elderlyId = ? ORDER BY followUpTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"medical_follow_up_record"}, new Callable<List<MedicalFollowUpRecord>>() {
      @Override
      @NonNull
      public List<MedicalFollowUpRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfFollowUpTime = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpTime");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<MedicalFollowUpRecord> _result = new ArrayList<MedicalFollowUpRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicalFollowUpRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final long _tmpFollowUpTime;
            _tmpFollowUpTime = _cursor.getLong(_cursorIndexOfFollowUpTime);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item = new MedicalFollowUpRecord(_tmpId,_tmpElderlyId,_tmpFollowUpTime,_tmpContent,_tmpStatus);
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
  public Flow<List<MedicalFollowUpRecord>> observeAll() {
    final String _sql = "SELECT * FROM medical_follow_up_record ORDER BY followUpTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"medical_follow_up_record"}, new Callable<List<MedicalFollowUpRecord>>() {
      @Override
      @NonNull
      public List<MedicalFollowUpRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfFollowUpTime = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpTime");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<MedicalFollowUpRecord> _result = new ArrayList<MedicalFollowUpRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicalFollowUpRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final long _tmpFollowUpTime;
            _tmpFollowUpTime = _cursor.getLong(_cursorIndexOfFollowUpTime);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item = new MedicalFollowUpRecord(_tmpId,_tmpElderlyId,_tmpFollowUpTime,_tmpContent,_tmpStatus);
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
  public Object getByElderlyIdAndTimeRange(final String elderlyId, final long startInclusive,
      final long endInclusive,
      final Continuation<? super List<MedicalFollowUpRecord>> $completion) {
    final String _sql = "SELECT * FROM medical_follow_up_record WHERE elderlyId = ? AND followUpTime >= ? AND followUpTime <= ? ORDER BY followUpTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startInclusive);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endInclusive);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicalFollowUpRecord>>() {
      @Override
      @NonNull
      public List<MedicalFollowUpRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfFollowUpTime = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpTime");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<MedicalFollowUpRecord> _result = new ArrayList<MedicalFollowUpRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicalFollowUpRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final long _tmpFollowUpTime;
            _tmpFollowUpTime = _cursor.getLong(_cursorIndexOfFollowUpTime);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item = new MedicalFollowUpRecord(_tmpId,_tmpElderlyId,_tmpFollowUpTime,_tmpContent,_tmpStatus);
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
  public Flow<Integer> observeCountByElderlyId(final String elderlyId) {
    final String _sql = "SELECT COUNT(*) FROM medical_follow_up_record WHERE elderlyId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"medical_follow_up_record"}, new Callable<Integer>() {
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
