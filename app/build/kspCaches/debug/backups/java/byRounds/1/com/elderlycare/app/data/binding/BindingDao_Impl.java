package com.elderlycare.app.data.binding;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class BindingDao_Impl implements BindingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<OrganizationEntity> __insertionAdapterOfOrganizationEntity;

  private final EntityInsertionAdapter<BindingRequestEntity> __insertionAdapterOfBindingRequestEntity;

  private final EntityInsertionAdapter<UserElderlyBindingEntity> __insertionAdapterOfUserElderlyBindingEntity;

  private final EntityInsertionAdapter<LocalAlertEntity> __insertionAdapterOfLocalAlertEntity;

  private final EntityDeletionOrUpdateAdapter<BindingRequestEntity> __updateAdapterOfBindingRequestEntity;

  private final EntityDeletionOrUpdateAdapter<UserElderlyBindingEntity> __updateAdapterOfUserElderlyBindingEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateRequestStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateBindingStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateAlertStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateAlertsByDevice;

  public BindingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfOrganizationEntity = new EntityInsertionAdapter<OrganizationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `organization` (`id`,`name`,`type`,`createdAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final OrganizationEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getType());
        statement.bindLong(4, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfBindingRequestEntity = new EntityInsertionAdapter<BindingRequestEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `binding_request` (`id`,`requesterUserId`,`requesterRole`,`organizationId`,`familyUserId`,`elderlyId`,`deviceId`,`status`,`message`,`createdAt`,`updatedAt`,`reviewedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BindingRequestEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getRequesterUserId());
        statement.bindString(3, entity.getRequesterRole());
        statement.bindString(4, entity.getOrganizationId());
        statement.bindString(5, entity.getFamilyUserId());
        statement.bindString(6, entity.getElderlyId());
        statement.bindString(7, entity.getDeviceId());
        statement.bindString(8, entity.getStatus());
        statement.bindString(9, entity.getMessage());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getUpdatedAt());
        if (entity.getReviewedAt() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getReviewedAt());
        }
      }
    };
    this.__insertionAdapterOfUserElderlyBindingEntity = new EntityInsertionAdapter<UserElderlyBindingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_elderly_binding` (`id`,`userId`,`userRole`,`organizationId`,`elderlyId`,`deviceId`,`permission`,`status`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserElderlyBindingEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getUserRole());
        statement.bindString(4, entity.getOrganizationId());
        statement.bindString(5, entity.getElderlyId());
        statement.bindString(6, entity.getDeviceId());
        statement.bindString(7, entity.getPermission());
        statement.bindString(8, entity.getStatus());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getUpdatedAt());
      }
    };
    this.__insertionAdapterOfLocalAlertEntity = new EntityInsertionAdapter<LocalAlertEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `local_alert` (`id`,`deviceId`,`elderlyId`,`type`,`level`,`content`,`timestamp`,`status`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalAlertEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDeviceId());
        statement.bindString(3, entity.getElderlyId());
        statement.bindString(4, entity.getType());
        statement.bindString(5, entity.getLevel());
        statement.bindString(6, entity.getContent());
        statement.bindLong(7, entity.getTimestamp());
        statement.bindString(8, entity.getStatus());
      }
    };
    this.__updateAdapterOfBindingRequestEntity = new EntityDeletionOrUpdateAdapter<BindingRequestEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `binding_request` SET `id` = ?,`requesterUserId` = ?,`requesterRole` = ?,`organizationId` = ?,`familyUserId` = ?,`elderlyId` = ?,`deviceId` = ?,`status` = ?,`message` = ?,`createdAt` = ?,`updatedAt` = ?,`reviewedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BindingRequestEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getRequesterUserId());
        statement.bindString(3, entity.getRequesterRole());
        statement.bindString(4, entity.getOrganizationId());
        statement.bindString(5, entity.getFamilyUserId());
        statement.bindString(6, entity.getElderlyId());
        statement.bindString(7, entity.getDeviceId());
        statement.bindString(8, entity.getStatus());
        statement.bindString(9, entity.getMessage());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getUpdatedAt());
        if (entity.getReviewedAt() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getReviewedAt());
        }
        statement.bindString(13, entity.getId());
      }
    };
    this.__updateAdapterOfUserElderlyBindingEntity = new EntityDeletionOrUpdateAdapter<UserElderlyBindingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `user_elderly_binding` SET `id` = ?,`userId` = ?,`userRole` = ?,`organizationId` = ?,`elderlyId` = ?,`deviceId` = ?,`permission` = ?,`status` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserElderlyBindingEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getUserRole());
        statement.bindString(4, entity.getOrganizationId());
        statement.bindString(5, entity.getElderlyId());
        statement.bindString(6, entity.getDeviceId());
        statement.bindString(7, entity.getPermission());
        statement.bindString(8, entity.getStatus());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getUpdatedAt());
        statement.bindString(11, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateRequestStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE binding_request SET status = ?, reviewedAt = ?, updatedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateBindingStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_elderly_binding SET status = ?, updatedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateAlertStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE local_alert SET status = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateAlertsByDevice = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE local_alert SET status = ? WHERE deviceId = ? AND status != ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrganization(final OrganizationEntity org,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfOrganizationEntity.insert(org);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertOrganizations(final List<OrganizationEntity> orgs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfOrganizationEntity.insert(orgs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertBindingRequest(final BindingRequestEntity request,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBindingRequestEntity.insert(request);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertBinding(final UserElderlyBindingEntity binding,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserElderlyBindingEntity.insert(binding);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAlert(final LocalAlertEntity alert,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalAlertEntity.insert(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAlerts(final List<LocalAlertEntity> alerts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalAlertEntity.insert(alerts);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBindingRequest(final BindingRequestEntity request,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfBindingRequestEntity.handle(request);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBinding(final UserElderlyBindingEntity binding,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfUserElderlyBindingEntity.handle(binding);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object approveAndCreateBinding(final BindingRequestEntity request,
      final UserElderlyBindingEntity binding, final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> BindingDao.DefaultImpls.approveAndCreateBinding(BindingDao_Impl.this, request, binding, __cont), $completion);
  }

  @Override
  public Object updateRequestStatus(final String id, final String status, final Long reviewedAt,
      final long updatedAt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateRequestStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        if (reviewedAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, reviewedAt);
        }
        _argIndex = 3;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 4;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfUpdateRequestStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBindingStatus(final String id, final String status, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateBindingStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 3;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfUpdateBindingStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAlertStatus(final String id, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateAlertStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfUpdateAlertStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAlertsByDevice(final String deviceId, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateAlertsByDevice.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindString(_argIndex, deviceId);
        _argIndex = 3;
        _stmt.bindString(_argIndex, status);
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
          __preparedStmtOfUpdateAlertsByDevice.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getOrganization(final String id,
      final Continuation<? super OrganizationEntity> $completion) {
    final String _sql = "SELECT * FROM organization WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<OrganizationEntity>() {
      @Override
      @Nullable
      public OrganizationEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final OrganizationEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new OrganizationEntity(_tmpId,_tmpName,_tmpType,_tmpCreatedAt);
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
  public Object getAllOrganizations(
      final Continuation<? super List<OrganizationEntity>> $completion) {
    final String _sql = "SELECT * FROM organization ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<OrganizationEntity>>() {
      @Override
      @NonNull
      public List<OrganizationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<OrganizationEntity> _result = new ArrayList<OrganizationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OrganizationEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new OrganizationEntity(_tmpId,_tmpName,_tmpType,_tmpCreatedAt);
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
  public Flow<List<OrganizationEntity>> observeAllOrganizations() {
    final String _sql = "SELECT * FROM organization ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"organization"}, new Callable<List<OrganizationEntity>>() {
      @Override
      @NonNull
      public List<OrganizationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<OrganizationEntity> _result = new ArrayList<OrganizationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OrganizationEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new OrganizationEntity(_tmpId,_tmpName,_tmpType,_tmpCreatedAt);
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
  public Object countOrganizations(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM organization";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
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
  public Object getBindingRequest(final String id,
      final Continuation<? super BindingRequestEntity> $completion) {
    final String _sql = "SELECT * FROM binding_request WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BindingRequestEntity>() {
      @Override
      @Nullable
      public BindingRequestEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRequesterUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterUserId");
          final int _cursorIndexOfRequesterRole = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfFamilyUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "familyUserId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfReviewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewedAt");
          final BindingRequestEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRequesterUserId;
            _tmpRequesterUserId = _cursor.getString(_cursorIndexOfRequesterUserId);
            final String _tmpRequesterRole;
            _tmpRequesterRole = _cursor.getString(_cursorIndexOfRequesterRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpFamilyUserId;
            _tmpFamilyUserId = _cursor.getString(_cursorIndexOfFamilyUserId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpMessage;
            _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpReviewedAt;
            if (_cursor.isNull(_cursorIndexOfReviewedAt)) {
              _tmpReviewedAt = null;
            } else {
              _tmpReviewedAt = _cursor.getLong(_cursorIndexOfReviewedAt);
            }
            _result = new BindingRequestEntity(_tmpId,_tmpRequesterUserId,_tmpRequesterRole,_tmpOrganizationId,_tmpFamilyUserId,_tmpElderlyId,_tmpDeviceId,_tmpStatus,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpReviewedAt);
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
  public Flow<List<BindingRequestEntity>> observeRequestsByFamilyUser(final String familyUserId) {
    final String _sql = "SELECT * FROM binding_request WHERE familyUserId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, familyUserId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"binding_request"}, new Callable<List<BindingRequestEntity>>() {
      @Override
      @NonNull
      public List<BindingRequestEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRequesterUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterUserId");
          final int _cursorIndexOfRequesterRole = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfFamilyUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "familyUserId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfReviewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewedAt");
          final List<BindingRequestEntity> _result = new ArrayList<BindingRequestEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BindingRequestEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRequesterUserId;
            _tmpRequesterUserId = _cursor.getString(_cursorIndexOfRequesterUserId);
            final String _tmpRequesterRole;
            _tmpRequesterRole = _cursor.getString(_cursorIndexOfRequesterRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpFamilyUserId;
            _tmpFamilyUserId = _cursor.getString(_cursorIndexOfFamilyUserId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpMessage;
            _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpReviewedAt;
            if (_cursor.isNull(_cursorIndexOfReviewedAt)) {
              _tmpReviewedAt = null;
            } else {
              _tmpReviewedAt = _cursor.getLong(_cursorIndexOfReviewedAt);
            }
            _item = new BindingRequestEntity(_tmpId,_tmpRequesterUserId,_tmpRequesterRole,_tmpOrganizationId,_tmpFamilyUserId,_tmpElderlyId,_tmpDeviceId,_tmpStatus,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpReviewedAt);
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
  public Object getRequestsByFamilyUser(final String familyUserId,
      final Continuation<? super List<BindingRequestEntity>> $completion) {
    final String _sql = "SELECT * FROM binding_request WHERE familyUserId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, familyUserId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BindingRequestEntity>>() {
      @Override
      @NonNull
      public List<BindingRequestEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRequesterUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterUserId");
          final int _cursorIndexOfRequesterRole = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfFamilyUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "familyUserId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfReviewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewedAt");
          final List<BindingRequestEntity> _result = new ArrayList<BindingRequestEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BindingRequestEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRequesterUserId;
            _tmpRequesterUserId = _cursor.getString(_cursorIndexOfRequesterUserId);
            final String _tmpRequesterRole;
            _tmpRequesterRole = _cursor.getString(_cursorIndexOfRequesterRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpFamilyUserId;
            _tmpFamilyUserId = _cursor.getString(_cursorIndexOfFamilyUserId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpMessage;
            _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpReviewedAt;
            if (_cursor.isNull(_cursorIndexOfReviewedAt)) {
              _tmpReviewedAt = null;
            } else {
              _tmpReviewedAt = _cursor.getLong(_cursorIndexOfReviewedAt);
            }
            _item = new BindingRequestEntity(_tmpId,_tmpRequesterUserId,_tmpRequesterRole,_tmpOrganizationId,_tmpFamilyUserId,_tmpElderlyId,_tmpDeviceId,_tmpStatus,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpReviewedAt);
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
  public Object getRequestsByRequester(final String requesterUserId,
      final Continuation<? super List<BindingRequestEntity>> $completion) {
    final String _sql = "SELECT * FROM binding_request WHERE requesterUserId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, requesterUserId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BindingRequestEntity>>() {
      @Override
      @NonNull
      public List<BindingRequestEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRequesterUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterUserId");
          final int _cursorIndexOfRequesterRole = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfFamilyUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "familyUserId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfReviewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewedAt");
          final List<BindingRequestEntity> _result = new ArrayList<BindingRequestEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BindingRequestEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRequesterUserId;
            _tmpRequesterUserId = _cursor.getString(_cursorIndexOfRequesterUserId);
            final String _tmpRequesterRole;
            _tmpRequesterRole = _cursor.getString(_cursorIndexOfRequesterRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpFamilyUserId;
            _tmpFamilyUserId = _cursor.getString(_cursorIndexOfFamilyUserId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpMessage;
            _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpReviewedAt;
            if (_cursor.isNull(_cursorIndexOfReviewedAt)) {
              _tmpReviewedAt = null;
            } else {
              _tmpReviewedAt = _cursor.getLong(_cursorIndexOfReviewedAt);
            }
            _item = new BindingRequestEntity(_tmpId,_tmpRequesterUserId,_tmpRequesterRole,_tmpOrganizationId,_tmpFamilyUserId,_tmpElderlyId,_tmpDeviceId,_tmpStatus,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpReviewedAt);
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
  public Object getRequestsByStatus(final String status,
      final Continuation<? super List<BindingRequestEntity>> $completion) {
    final String _sql = "SELECT * FROM binding_request WHERE status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, status);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BindingRequestEntity>>() {
      @Override
      @NonNull
      public List<BindingRequestEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRequesterUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterUserId");
          final int _cursorIndexOfRequesterRole = CursorUtil.getColumnIndexOrThrow(_cursor, "requesterRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfFamilyUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "familyUserId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfReviewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewedAt");
          final List<BindingRequestEntity> _result = new ArrayList<BindingRequestEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BindingRequestEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRequesterUserId;
            _tmpRequesterUserId = _cursor.getString(_cursorIndexOfRequesterUserId);
            final String _tmpRequesterRole;
            _tmpRequesterRole = _cursor.getString(_cursorIndexOfRequesterRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpFamilyUserId;
            _tmpFamilyUserId = _cursor.getString(_cursorIndexOfFamilyUserId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpMessage;
            _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpReviewedAt;
            if (_cursor.isNull(_cursorIndexOfReviewedAt)) {
              _tmpReviewedAt = null;
            } else {
              _tmpReviewedAt = _cursor.getLong(_cursorIndexOfReviewedAt);
            }
            _item = new BindingRequestEntity(_tmpId,_tmpRequesterUserId,_tmpRequesterRole,_tmpOrganizationId,_tmpFamilyUserId,_tmpElderlyId,_tmpDeviceId,_tmpStatus,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpReviewedAt);
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
  public Object getBinding(final String id,
      final Continuation<? super UserElderlyBindingEntity> $completion) {
    final String _sql = "SELECT * FROM user_elderly_binding WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserElderlyBindingEntity>() {
      @Override
      @Nullable
      public UserElderlyBindingEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserRole = CursorUtil.getColumnIndexOrThrow(_cursor, "userRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfPermission = CursorUtil.getColumnIndexOrThrow(_cursor, "permission");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final UserElderlyBindingEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserRole;
            _tmpUserRole = _cursor.getString(_cursorIndexOfUserRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpPermission;
            _tmpPermission = _cursor.getString(_cursorIndexOfPermission);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserElderlyBindingEntity(_tmpId,_tmpUserId,_tmpUserRole,_tmpOrganizationId,_tmpElderlyId,_tmpDeviceId,_tmpPermission,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getBindingsByUser(final String userId, final String status,
      final Continuation<? super List<UserElderlyBindingEntity>> $completion) {
    final String _sql = "SELECT * FROM user_elderly_binding WHERE userId = ? AND status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, status);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<UserElderlyBindingEntity>>() {
      @Override
      @NonNull
      public List<UserElderlyBindingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserRole = CursorUtil.getColumnIndexOrThrow(_cursor, "userRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfPermission = CursorUtil.getColumnIndexOrThrow(_cursor, "permission");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<UserElderlyBindingEntity> _result = new ArrayList<UserElderlyBindingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserElderlyBindingEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserRole;
            _tmpUserRole = _cursor.getString(_cursorIndexOfUserRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpPermission;
            _tmpPermission = _cursor.getString(_cursorIndexOfPermission);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new UserElderlyBindingEntity(_tmpId,_tmpUserId,_tmpUserRole,_tmpOrganizationId,_tmpElderlyId,_tmpDeviceId,_tmpPermission,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<UserElderlyBindingEntity>> observeBindingsByUser(final String userId,
      final String status) {
    final String _sql = "SELECT * FROM user_elderly_binding WHERE userId = ? AND status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, status);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_elderly_binding"}, new Callable<List<UserElderlyBindingEntity>>() {
      @Override
      @NonNull
      public List<UserElderlyBindingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserRole = CursorUtil.getColumnIndexOrThrow(_cursor, "userRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfPermission = CursorUtil.getColumnIndexOrThrow(_cursor, "permission");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<UserElderlyBindingEntity> _result = new ArrayList<UserElderlyBindingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserElderlyBindingEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserRole;
            _tmpUserRole = _cursor.getString(_cursorIndexOfUserRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpPermission;
            _tmpPermission = _cursor.getString(_cursorIndexOfPermission);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new UserElderlyBindingEntity(_tmpId,_tmpUserId,_tmpUserRole,_tmpOrganizationId,_tmpElderlyId,_tmpDeviceId,_tmpPermission,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getBindingsByElderly(final String elderlyId, final String status,
      final Continuation<? super List<UserElderlyBindingEntity>> $completion) {
    final String _sql = "SELECT * FROM user_elderly_binding WHERE elderlyId = ? AND status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    _argIndex = 2;
    _statement.bindString(_argIndex, status);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<UserElderlyBindingEntity>>() {
      @Override
      @NonNull
      public List<UserElderlyBindingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserRole = CursorUtil.getColumnIndexOrThrow(_cursor, "userRole");
          final int _cursorIndexOfOrganizationId = CursorUtil.getColumnIndexOrThrow(_cursor, "organizationId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfPermission = CursorUtil.getColumnIndexOrThrow(_cursor, "permission");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<UserElderlyBindingEntity> _result = new ArrayList<UserElderlyBindingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserElderlyBindingEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserRole;
            _tmpUserRole = _cursor.getString(_cursorIndexOfUserRole);
            final String _tmpOrganizationId;
            _tmpOrganizationId = _cursor.getString(_cursorIndexOfOrganizationId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpPermission;
            _tmpPermission = _cursor.getString(_cursorIndexOfPermission);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new UserElderlyBindingEntity(_tmpId,_tmpUserId,_tmpUserRole,_tmpOrganizationId,_tmpElderlyId,_tmpDeviceId,_tmpPermission,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getAlert(final String id,
      final Continuation<? super LocalAlertEntity> $completion) {
    final String _sql = "SELECT * FROM local_alert WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalAlertEntity>() {
      @Override
      @Nullable
      public LocalAlertEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final LocalAlertEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpLevel;
            _tmpLevel = _cursor.getString(_cursorIndexOfLevel);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _result = new LocalAlertEntity(_tmpId,_tmpDeviceId,_tmpElderlyId,_tmpType,_tmpLevel,_tmpContent,_tmpTimestamp,_tmpStatus);
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
  public Object getAlertsByElderly(final String elderlyId,
      final Continuation<? super List<LocalAlertEntity>> $completion) {
    final String _sql = "SELECT * FROM local_alert WHERE elderlyId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, elderlyId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalAlertEntity>>() {
      @Override
      @NonNull
      public List<LocalAlertEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<LocalAlertEntity> _result = new ArrayList<LocalAlertEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalAlertEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpLevel;
            _tmpLevel = _cursor.getString(_cursorIndexOfLevel);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item = new LocalAlertEntity(_tmpId,_tmpDeviceId,_tmpElderlyId,_tmpType,_tmpLevel,_tmpContent,_tmpTimestamp,_tmpStatus);
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
  public Flow<List<LocalAlertEntity>> observeAlertsByElderlies(final List<String> elderlyIds) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM local_alert WHERE elderlyId IN (");
    final int _inputSize = elderlyIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") ORDER BY timestamp DESC");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : elderlyIds) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"local_alert"}, new Callable<List<LocalAlertEntity>>() {
      @Override
      @NonNull
      public List<LocalAlertEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<LocalAlertEntity> _result = new ArrayList<LocalAlertEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalAlertEntity _item_1;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpLevel;
            _tmpLevel = _cursor.getString(_cursorIndexOfLevel);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item_1 = new LocalAlertEntity(_tmpId,_tmpDeviceId,_tmpElderlyId,_tmpType,_tmpLevel,_tmpContent,_tmpTimestamp,_tmpStatus);
            _result.add(_item_1);
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
  public Object getAlertsByDevice(final String deviceId,
      final Continuation<? super List<LocalAlertEntity>> $completion) {
    final String _sql = "SELECT * FROM local_alert WHERE deviceId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalAlertEntity>>() {
      @Override
      @NonNull
      public List<LocalAlertEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfElderlyId = CursorUtil.getColumnIndexOrThrow(_cursor, "elderlyId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<LocalAlertEntity> _result = new ArrayList<LocalAlertEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalAlertEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDeviceId;
            _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            final String _tmpElderlyId;
            _tmpElderlyId = _cursor.getString(_cursorIndexOfElderlyId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpLevel;
            _tmpLevel = _cursor.getString(_cursorIndexOfLevel);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item = new LocalAlertEntity(_tmpId,_tmpDeviceId,_tmpElderlyId,_tmpType,_tmpLevel,_tmpContent,_tmpTimestamp,_tmpStatus);
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
