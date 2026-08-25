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
public final class HealthAdviceDao_Impl implements HealthAdviceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HealthAdvice> __insertionAdapterOfHealthAdvice;

  public HealthAdviceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHealthAdvice = new EntityInsertionAdapter<HealthAdvice>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `health_advice` (`id`,`elderlyId`,`adviceTime`,`adviceContent`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthAdvice entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getElderlyId());
        statement.bindLong(3, entity.getAdviceTime());
        statement.bindString(4, entity.getAdviceContent());
      }
    };
  }

  @Override
  public Object insert(final HealthAdvice advice, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHealthAdvice.insertAndReturnId(advice);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HealthAdvice>> observeByElderlyId(final String elderlyId) {
    final String _sql = "SELECT * FROM health_advice WHERE elderlyId = ? ORDER BY adviceTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"health_advice"}, new Callable<List<HealthAdvice>>() {
      @Override
      @NonNull
      public List<HealthAdvice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfAdviceTime = CursorUtil.getColumnIndexOrThrow(_cursor, "adviceTime");
          final int _cursorIndexOfAdviceContent = CursorUtil.getColumnIndexOrThrow(_cursor, "adviceContent");
          final List<HealthAdvice> _result = new ArrayList<HealthAdvice>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HealthAdvice _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final long _tmpAdviceTime;
            _tmpAdviceTime = _cursor.getLong(_cursorIndexOfAdviceTime);
            final String _tmpAdviceContent;
            _tmpAdviceContent = _cursor.getString(_cursorIndexOfAdviceContent);
            _item = new HealthAdvice(_tmpId,_tmpElderlyId,_tmpAdviceTime,_tmpAdviceContent);
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
  public Object getByElderlyId(final String elderlyId,
      final Continuation<? super List<HealthAdvice>> $completion) {
    final String _sql = "SELECT * FROM health_advice WHERE elderlyId = ? ORDER BY adviceTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HealthAdvice>>() {
      @Override
      @NonNull
      public List<HealthAdvice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfAdviceTime = CursorUtil.getColumnIndexOrThrow(_cursor, "adviceTime");
          final int _cursorIndexOfAdviceContent = CursorUtil.getColumnIndexOrThrow(_cursor, "adviceContent");
          final List<HealthAdvice> _result = new ArrayList<HealthAdvice>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HealthAdvice _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final long _tmpAdviceTime;
            _tmpAdviceTime = _cursor.getLong(_cursorIndexOfAdviceTime);
            final String _tmpAdviceContent;
            _tmpAdviceContent = _cursor.getString(_cursorIndexOfAdviceContent);
            _item = new HealthAdvice(_tmpId,_tmpElderlyId,_tmpAdviceTime,_tmpAdviceContent);
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
    final String _sql = "SELECT COUNT(*) FROM health_advice WHERE elderlyId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"health_advice"}, new Callable<Integer>() {
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
