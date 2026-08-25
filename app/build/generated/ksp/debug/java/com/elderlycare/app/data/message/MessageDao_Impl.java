package com.elderlycare.app.data.message;

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
public final class MessageDao_Impl implements MessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity_1;

  private final EntityDeletionOrUpdateAdapter<MessageEntity> __deletionAdapterOfMessageEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateAudioInfo;

  private final SharedSQLiteStatement __preparedStmtOfMarkAsRead;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSendStatus;

  private final SharedSQLiteStatement __preparedStmtOfMarkAllRead;

  private final SharedSQLiteStatement __preparedStmtOfMarkAllReadByCategory;

  private final SharedSQLiteStatement __preparedStmtOfUpdateRemoteId;

  private final SharedSQLiteStatement __preparedStmtOfUpdateIsReadByRemoteId;

  private final SharedSQLiteStatement __preparedStmtOfMarkBindingMessagesRead;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessageEntity = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `message` (`id`,`msgType`,`senderName`,`content`,`localAudioPath`,`duration`,`createTime`,`isRead`,`deviceSerial`,`remoteId`,`sendStatus`,`sendChannel`,`failReason`,`localVideoPath`,`videoCloudUrl`,`thumbUrl`,`messageCategory`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getMsgType());
        statement.bindString(3, entity.getSenderName());
        statement.bindString(4, entity.getContent());
        statement.bindString(5, entity.getLocalAudioPath());
        statement.bindLong(6, entity.getDuration());
        statement.bindLong(7, entity.getCreateTime());
        final int _tmp = entity.isRead() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindString(9, entity.getDeviceSerial());
        statement.bindString(10, entity.getRemoteId());
        statement.bindLong(11, entity.getSendStatus());
        statement.bindLong(12, entity.getSendChannel());
        statement.bindString(13, entity.getFailReason());
        statement.bindString(14, entity.getLocalVideoPath());
        statement.bindString(15, entity.getVideoCloudUrl());
        statement.bindString(16, entity.getThumbUrl());
        statement.bindLong(17, entity.getMessageCategory());
      }
    };
    this.__insertionAdapterOfMessageEntity_1 = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `message` (`id`,`msgType`,`senderName`,`content`,`localAudioPath`,`duration`,`createTime`,`isRead`,`deviceSerial`,`remoteId`,`sendStatus`,`sendChannel`,`failReason`,`localVideoPath`,`videoCloudUrl`,`thumbUrl`,`messageCategory`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getMsgType());
        statement.bindString(3, entity.getSenderName());
        statement.bindString(4, entity.getContent());
        statement.bindString(5, entity.getLocalAudioPath());
        statement.bindLong(6, entity.getDuration());
        statement.bindLong(7, entity.getCreateTime());
        final int _tmp = entity.isRead() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindString(9, entity.getDeviceSerial());
        statement.bindString(10, entity.getRemoteId());
        statement.bindLong(11, entity.getSendStatus());
        statement.bindLong(12, entity.getSendChannel());
        statement.bindString(13, entity.getFailReason());
        statement.bindString(14, entity.getLocalVideoPath());
        statement.bindString(15, entity.getVideoCloudUrl());
        statement.bindString(16, entity.getThumbUrl());
        statement.bindLong(17, entity.getMessageCategory());
      }
    };
    this.__deletionAdapterOfMessageEntity = new EntityDeletionOrUpdateAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `message` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateAudioInfo = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE message SET localAudioPath = ?, duration = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkAsRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE message SET isRead = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateSendStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE message SET sendStatus = ?, sendChannel = ?, failReason = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkAllRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE message SET isRead = 1 WHERE deviceSerial = ? AND isRead = 0";
        return _query;
      }
    };
    this.__preparedStmtOfMarkAllReadByCategory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE message SET isRead = 1 WHERE deviceSerial = ? AND messageCategory = ? AND isRead = 0";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateRemoteId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE message SET remoteId = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateIsReadByRemoteId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE message SET isRead = ? WHERE remoteId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkBindingMessagesRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE message SET isRead = 1 WHERE msgType = 4 AND remoteId GLOB 'binding_*' AND isRead = 0";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MessageEntity message, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMessageEntity.insertAndReturnId(message);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertIgnore(final MessageEntity message,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMessageEntity_1.insertAndReturnId(message);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final MessageEntity message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMessageEntity.handle(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAudioInfo(final long id, final String path, final int duration,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateAudioInfo.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, path);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, duration);
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
          __preparedStmtOfUpdateAudioInfo.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsRead(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAsRead.acquire();
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
          __preparedStmtOfMarkAsRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSendStatus(final long id, final int status, final int channel,
      final String failReason, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSendStatus.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, channel);
        _argIndex = 3;
        _stmt.bindString(_argIndex, failReason);
        _argIndex = 4;
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
          __preparedStmtOfUpdateSendStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markAllRead(final String deviceSerial,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAllRead.acquire();
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
          __preparedStmtOfMarkAllRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markAllReadByCategory(final String deviceSerial, final int category,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAllReadByCategory.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, deviceSerial);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, category);
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
          __preparedStmtOfMarkAllReadByCategory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRemoteId(final long id, final String remoteId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateRemoteId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, remoteId);
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
          __preparedStmtOfUpdateRemoteId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateIsReadByRemoteId(final String remoteId, final boolean isRead,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateIsReadByRemoteId.acquire();
        int _argIndex = 1;
        final int _tmp = isRead ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, remoteId);
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
          __preparedStmtOfUpdateIsReadByRemoteId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markBindingMessagesRead(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkBindingMessagesRead.acquire();
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
          __preparedStmtOfMarkBindingMessagesRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByRemoteId(final String remoteId,
      final Continuation<? super MessageEntity> $completion) {
    final String _sql = "SELECT * FROM message WHERE remoteId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, remoteId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MessageEntity>() {
      @Override
      @Nullable
      public MessageEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMsgType = CursorUtil.getColumnIndexOrThrow(_cursor, "msgType");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLocalAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localAudioPath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfRemoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteId");
          final int _cursorIndexOfSendStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sendStatus");
          final int _cursorIndexOfSendChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "sendChannel");
          final int _cursorIndexOfFailReason = CursorUtil.getColumnIndexOrThrow(_cursor, "failReason");
          final int _cursorIndexOfLocalVideoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localVideoPath");
          final int _cursorIndexOfVideoCloudUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoCloudUrl");
          final int _cursorIndexOfThumbUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbUrl");
          final int _cursorIndexOfMessageCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "messageCategory");
          final MessageEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpMsgType;
            _tmpMsgType = _cursor.getInt(_cursorIndexOfMsgType);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpLocalAudioPath;
            _tmpLocalAudioPath = _cursor.getString(_cursorIndexOfLocalAudioPath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final String _tmpRemoteId;
            _tmpRemoteId = _cursor.getString(_cursorIndexOfRemoteId);
            final int _tmpSendStatus;
            _tmpSendStatus = _cursor.getInt(_cursorIndexOfSendStatus);
            final int _tmpSendChannel;
            _tmpSendChannel = _cursor.getInt(_cursorIndexOfSendChannel);
            final String _tmpFailReason;
            _tmpFailReason = _cursor.getString(_cursorIndexOfFailReason);
            final String _tmpLocalVideoPath;
            _tmpLocalVideoPath = _cursor.getString(_cursorIndexOfLocalVideoPath);
            final String _tmpVideoCloudUrl;
            _tmpVideoCloudUrl = _cursor.getString(_cursorIndexOfVideoCloudUrl);
            final String _tmpThumbUrl;
            _tmpThumbUrl = _cursor.getString(_cursorIndexOfThumbUrl);
            final int _tmpMessageCategory;
            _tmpMessageCategory = _cursor.getInt(_cursorIndexOfMessageCategory);
            _result = new MessageEntity(_tmpId,_tmpMsgType,_tmpSenderName,_tmpContent,_tmpLocalAudioPath,_tmpDuration,_tmpCreateTime,_tmpIsRead,_tmpDeviceSerial,_tmpRemoteId,_tmpSendStatus,_tmpSendChannel,_tmpFailReason,_tmpLocalVideoPath,_tmpVideoCloudUrl,_tmpThumbUrl,_tmpMessageCategory);
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
  public Object getAllByDeviceSerial(final String deviceSerial,
      final Continuation<? super List<MessageEntity>> $completion) {
    final String _sql = "SELECT * FROM message WHERE deviceSerial = ? ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceSerial);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMsgType = CursorUtil.getColumnIndexOrThrow(_cursor, "msgType");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLocalAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localAudioPath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfRemoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteId");
          final int _cursorIndexOfSendStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sendStatus");
          final int _cursorIndexOfSendChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "sendChannel");
          final int _cursorIndexOfFailReason = CursorUtil.getColumnIndexOrThrow(_cursor, "failReason");
          final int _cursorIndexOfLocalVideoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localVideoPath");
          final int _cursorIndexOfVideoCloudUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoCloudUrl");
          final int _cursorIndexOfThumbUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbUrl");
          final int _cursorIndexOfMessageCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "messageCategory");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpMsgType;
            _tmpMsgType = _cursor.getInt(_cursorIndexOfMsgType);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpLocalAudioPath;
            _tmpLocalAudioPath = _cursor.getString(_cursorIndexOfLocalAudioPath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final String _tmpRemoteId;
            _tmpRemoteId = _cursor.getString(_cursorIndexOfRemoteId);
            final int _tmpSendStatus;
            _tmpSendStatus = _cursor.getInt(_cursorIndexOfSendStatus);
            final int _tmpSendChannel;
            _tmpSendChannel = _cursor.getInt(_cursorIndexOfSendChannel);
            final String _tmpFailReason;
            _tmpFailReason = _cursor.getString(_cursorIndexOfFailReason);
            final String _tmpLocalVideoPath;
            _tmpLocalVideoPath = _cursor.getString(_cursorIndexOfLocalVideoPath);
            final String _tmpVideoCloudUrl;
            _tmpVideoCloudUrl = _cursor.getString(_cursorIndexOfVideoCloudUrl);
            final String _tmpThumbUrl;
            _tmpThumbUrl = _cursor.getString(_cursorIndexOfThumbUrl);
            final int _tmpMessageCategory;
            _tmpMessageCategory = _cursor.getInt(_cursorIndexOfMessageCategory);
            _item = new MessageEntity(_tmpId,_tmpMsgType,_tmpSenderName,_tmpContent,_tmpLocalAudioPath,_tmpDuration,_tmpCreateTime,_tmpIsRead,_tmpDeviceSerial,_tmpRemoteId,_tmpSendStatus,_tmpSendChannel,_tmpFailReason,_tmpLocalVideoPath,_tmpVideoCloudUrl,_tmpThumbUrl,_tmpMessageCategory);
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
  public Flow<List<MessageEntity>> observeByDeviceSerial(final String deviceSerial) {
    final String _sql = "SELECT * FROM message WHERE deviceSerial = ? ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceSerial);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"message"}, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMsgType = CursorUtil.getColumnIndexOrThrow(_cursor, "msgType");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLocalAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localAudioPath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfRemoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteId");
          final int _cursorIndexOfSendStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sendStatus");
          final int _cursorIndexOfSendChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "sendChannel");
          final int _cursorIndexOfFailReason = CursorUtil.getColumnIndexOrThrow(_cursor, "failReason");
          final int _cursorIndexOfLocalVideoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localVideoPath");
          final int _cursorIndexOfVideoCloudUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoCloudUrl");
          final int _cursorIndexOfThumbUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbUrl");
          final int _cursorIndexOfMessageCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "messageCategory");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpMsgType;
            _tmpMsgType = _cursor.getInt(_cursorIndexOfMsgType);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpLocalAudioPath;
            _tmpLocalAudioPath = _cursor.getString(_cursorIndexOfLocalAudioPath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final String _tmpRemoteId;
            _tmpRemoteId = _cursor.getString(_cursorIndexOfRemoteId);
            final int _tmpSendStatus;
            _tmpSendStatus = _cursor.getInt(_cursorIndexOfSendStatus);
            final int _tmpSendChannel;
            _tmpSendChannel = _cursor.getInt(_cursorIndexOfSendChannel);
            final String _tmpFailReason;
            _tmpFailReason = _cursor.getString(_cursorIndexOfFailReason);
            final String _tmpLocalVideoPath;
            _tmpLocalVideoPath = _cursor.getString(_cursorIndexOfLocalVideoPath);
            final String _tmpVideoCloudUrl;
            _tmpVideoCloudUrl = _cursor.getString(_cursorIndexOfVideoCloudUrl);
            final String _tmpThumbUrl;
            _tmpThumbUrl = _cursor.getString(_cursorIndexOfThumbUrl);
            final int _tmpMessageCategory;
            _tmpMessageCategory = _cursor.getInt(_cursorIndexOfMessageCategory);
            _item = new MessageEntity(_tmpId,_tmpMsgType,_tmpSenderName,_tmpContent,_tmpLocalAudioPath,_tmpDuration,_tmpCreateTime,_tmpIsRead,_tmpDeviceSerial,_tmpRemoteId,_tmpSendStatus,_tmpSendChannel,_tmpFailReason,_tmpLocalVideoPath,_tmpVideoCloudUrl,_tmpThumbUrl,_tmpMessageCategory);
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
  public Flow<Integer> observeUnreadCount(final String deviceSerial) {
    final String _sql = "SELECT COUNT(*) FROM message WHERE deviceSerial = ? AND isRead = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceSerial);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"message"}, new Callable<Integer>() {
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

  @Override
  public Object getById(final long id, final Continuation<? super MessageEntity> $completion) {
    final String _sql = "SELECT * FROM message WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MessageEntity>() {
      @Override
      @Nullable
      public MessageEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMsgType = CursorUtil.getColumnIndexOrThrow(_cursor, "msgType");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLocalAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localAudioPath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfRemoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteId");
          final int _cursorIndexOfSendStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sendStatus");
          final int _cursorIndexOfSendChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "sendChannel");
          final int _cursorIndexOfFailReason = CursorUtil.getColumnIndexOrThrow(_cursor, "failReason");
          final int _cursorIndexOfLocalVideoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localVideoPath");
          final int _cursorIndexOfVideoCloudUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoCloudUrl");
          final int _cursorIndexOfThumbUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbUrl");
          final int _cursorIndexOfMessageCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "messageCategory");
          final MessageEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpMsgType;
            _tmpMsgType = _cursor.getInt(_cursorIndexOfMsgType);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpLocalAudioPath;
            _tmpLocalAudioPath = _cursor.getString(_cursorIndexOfLocalAudioPath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final String _tmpRemoteId;
            _tmpRemoteId = _cursor.getString(_cursorIndexOfRemoteId);
            final int _tmpSendStatus;
            _tmpSendStatus = _cursor.getInt(_cursorIndexOfSendStatus);
            final int _tmpSendChannel;
            _tmpSendChannel = _cursor.getInt(_cursorIndexOfSendChannel);
            final String _tmpFailReason;
            _tmpFailReason = _cursor.getString(_cursorIndexOfFailReason);
            final String _tmpLocalVideoPath;
            _tmpLocalVideoPath = _cursor.getString(_cursorIndexOfLocalVideoPath);
            final String _tmpVideoCloudUrl;
            _tmpVideoCloudUrl = _cursor.getString(_cursorIndexOfVideoCloudUrl);
            final String _tmpThumbUrl;
            _tmpThumbUrl = _cursor.getString(_cursorIndexOfThumbUrl);
            final int _tmpMessageCategory;
            _tmpMessageCategory = _cursor.getInt(_cursorIndexOfMessageCategory);
            _result = new MessageEntity(_tmpId,_tmpMsgType,_tmpSenderName,_tmpContent,_tmpLocalAudioPath,_tmpDuration,_tmpCreateTime,_tmpIsRead,_tmpDeviceSerial,_tmpRemoteId,_tmpSendStatus,_tmpSendChannel,_tmpFailReason,_tmpLocalVideoPath,_tmpVideoCloudUrl,_tmpThumbUrl,_tmpMessageCategory);
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
  public Flow<List<MessageEntity>> observeByDeviceSerialAndCategory(final String deviceSerial,
      final int category) {
    final String _sql = "SELECT * FROM message WHERE deviceSerial = ? AND messageCategory = ? ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceSerial);
    _argIndex = 2;
    _statement.bindLong(_argIndex, category);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"message"}, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMsgType = CursorUtil.getColumnIndexOrThrow(_cursor, "msgType");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfLocalAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localAudioPath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfDeviceSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceSerial");
          final int _cursorIndexOfRemoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteId");
          final int _cursorIndexOfSendStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sendStatus");
          final int _cursorIndexOfSendChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "sendChannel");
          final int _cursorIndexOfFailReason = CursorUtil.getColumnIndexOrThrow(_cursor, "failReason");
          final int _cursorIndexOfLocalVideoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localVideoPath");
          final int _cursorIndexOfVideoCloudUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoCloudUrl");
          final int _cursorIndexOfThumbUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbUrl");
          final int _cursorIndexOfMessageCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "messageCategory");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpMsgType;
            _tmpMsgType = _cursor.getInt(_cursorIndexOfMsgType);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpLocalAudioPath;
            _tmpLocalAudioPath = _cursor.getString(_cursorIndexOfLocalAudioPath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpCreateTime;
            _tmpCreateTime = _cursor.getLong(_cursorIndexOfCreateTime);
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final String _tmpDeviceSerial;
            _tmpDeviceSerial = _cursor.getString(_cursorIndexOfDeviceSerial);
            final String _tmpRemoteId;
            _tmpRemoteId = _cursor.getString(_cursorIndexOfRemoteId);
            final int _tmpSendStatus;
            _tmpSendStatus = _cursor.getInt(_cursorIndexOfSendStatus);
            final int _tmpSendChannel;
            _tmpSendChannel = _cursor.getInt(_cursorIndexOfSendChannel);
            final String _tmpFailReason;
            _tmpFailReason = _cursor.getString(_cursorIndexOfFailReason);
            final String _tmpLocalVideoPath;
            _tmpLocalVideoPath = _cursor.getString(_cursorIndexOfLocalVideoPath);
            final String _tmpVideoCloudUrl;
            _tmpVideoCloudUrl = _cursor.getString(_cursorIndexOfVideoCloudUrl);
            final String _tmpThumbUrl;
            _tmpThumbUrl = _cursor.getString(_cursorIndexOfThumbUrl);
            final int _tmpMessageCategory;
            _tmpMessageCategory = _cursor.getInt(_cursorIndexOfMessageCategory);
            _item = new MessageEntity(_tmpId,_tmpMsgType,_tmpSenderName,_tmpContent,_tmpLocalAudioPath,_tmpDuration,_tmpCreateTime,_tmpIsRead,_tmpDeviceSerial,_tmpRemoteId,_tmpSendStatus,_tmpSendChannel,_tmpFailReason,_tmpLocalVideoPath,_tmpVideoCloudUrl,_tmpThumbUrl,_tmpMessageCategory);
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
  public Flow<Integer> observeUnreadCountByCategory(final String deviceSerial, final int category) {
    final String _sql = "SELECT COUNT(*) FROM message WHERE deviceSerial = ? AND messageCategory = ? AND isRead = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceSerial);
    _argIndex = 2;
    _statement.bindLong(_argIndex, category);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"message"}, new Callable<Integer>() {
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

  @Override
  public Flow<Integer> observeUnreadBindingCount() {
    final String _sql = "SELECT COUNT(*) FROM message WHERE msgType = 4 AND remoteId GLOB 'binding_*' AND isRead = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"message"}, new Callable<Integer>() {
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

  @Override
  public Object markAsReadByIds(final List<Long> ids,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE message SET isRead = 1 WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(") AND isRead = 0");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
