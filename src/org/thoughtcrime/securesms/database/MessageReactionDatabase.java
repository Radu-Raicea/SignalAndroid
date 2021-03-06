package org.thoughtcrime.securesms.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Parcel;
import android.support.annotation.NonNull;

import org.thoughtcrime.securesms.database.model.MessageRecord;

public class MessageReactionDatabase extends Database {
    private static final String TAG = MessageReactionDatabase.class.getSimpleName();

    public static final String TABLE_NAME    = "reactions";
    public static final String ID            = "id";
    public static final String SMS_HASH      = "sms_id";
    public static final String MMS_HASH      = "mms_id";
    public static final String REACTION      = "reaction";
    public static final String REACTOR_ID    = "reactor_id";
    public static final String HASH_ID       = "hash";
    public static final String REACTION_DATE = "date_sent";

    public static final String[] PROJECTION = {
            MessageReactionDatabase.REACTION,
            MessageReactionDatabase.SMS_HASH,
            MessageReactionDatabase.MMS_HASH,
            MessageReactionDatabase.REACTION,
            MessageReactionDatabase.REACTOR_ID,
            MessageReactionDatabase.REACTION_DATE,
            MessageReactionDatabase.HASH_ID
    };

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" +
            ID + "  INTEGER PRIMARY KEY, " +
            SMS_HASH + " TEXT DEFAULT NULL, " +
            MMS_HASH + " TEXT DEFAULT NULL, " +
            REACTION + " TEXT NOT NULL, " +
            REACTOR_ID + " TEXT NOT NULL, " +
            REACTION_DATE + " INTEGER NOT NULL, " +
            "FOREIGN KEY(" + SMS_HASH + ") REFERENCES " + SmsDatabase.TABLE_NAME + "(" + SmsDatabase.HASH + ") ON DELETE CASCADE," +
            "FOREIGN KEY(" + MMS_HASH + ") REFERENCES " + MmsDatabase.TABLE_NAME + "(" + MmsDatabase.HASH + ") ON DELETE CASCADE);";

    public MessageReactionDatabase(Context context, SQLiteOpenHelper databaseHelper) {
        super(context, databaseHelper);
    }

    public void reactToMessage(String hash, String reaction, Long reactionDate, String reactorID, Long threadId) {
        SQLiteDatabase  db = databaseHelper.getWritableDatabase();
        ContentValues   values = new ContentValues();
        String          type = this.getMessageType(hash);

        values.put(type, hash);
        values.put(REACTION, reaction);
        values.put(REACTOR_ID, reactorID);
        values.put(REACTION_DATE, reactionDate);
        insertOrUpdate(values, type);
        notifyConversationListeners(threadId);
    }

    public void reactToMessage(MessageRecord messageRecord, String reaction, Long reactionDate) {
        String        messageType = getMessageType(messageRecord.getHash());
        ContentValues values      = new ContentValues();
        values.put(messageType, messageRecord.getHash());

        String address;

        try {
            address = DatabaseFactory.getIdentityDatabase(context).getMyIdentity().getAddress().serialize();
        } catch (Exception e) {
            e.printStackTrace();
            Parcel p = Parcel.obtain();
            p.writeString("Me");
            p.setDataPosition(0);
            address = new Address(p).serialize();
        }

        values.put(REACTION, reaction);
        values.put(REACTOR_ID, address);
        values.put(REACTION_DATE, reactionDate);
        this.insertOrUpdate(values, messageType);
    }

    private void insertOrUpdate(ContentValues values, String messageType) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        database.beginTransaction();

        int updated = database.update(TABLE_NAME, values, messageType + " = ? AND " +
                        REACTOR_ID + " = ? AND " +
                        REACTION + " = ?",
                new String[]{values.getAsString(messageType) + "",
                        values.getAsString(REACTOR_ID),
                        values.getAsString(REACTION)});

        if (updated < 1) {
            database.insert(TABLE_NAME, null, values);
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public int removeReaction(MessageRecord record) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        return database.delete(TABLE_NAME, this.getMessageType(record.getHash()) + " = ?", new String[]{record.getHash()});
    }

    public int removeDanglingSmsReactions() {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        return database.delete(TABLE_NAME,SMS_HASH + " in (SELECT " + SMS_HASH + " FROM " + TABLE_NAME + " OUTER LEFT JOIN "
                        + SmsDatabase.TABLE_NAME + " ON " + TABLE_NAME + "." + SMS_HASH + " = sms.hash WHERE sms.hash is null)",
                new String[]{});
    }

    public int removeDanglingMmsReactions() {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        return database.delete(TABLE_NAME,MMS_HASH + " in (SELECT " + MMS_HASH+ " FROM " + TABLE_NAME + " OUTER LEFT JOIN "
                        + MmsDatabase.TABLE_NAME + " ON " + TABLE_NAME + "." + MMS_HASH + " = mms.hash WHERE mms.hash is null)",
                new String[]{});
    }

    public Cursor getMessageReaction(MessageRecord messageRecord) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String hash = messageRecord.getHash();

        if (hash == null) return null;

        return database.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " +
                this.getMessageType(messageRecord.getHash()) + " = ?", new String[]{messageRecord.getHash()});
    }

    @NonNull
    private String getMessageType(String hash) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        return db.rawQuery("SELECT " + MmsSmsColumns.HASH + " FROM " + SmsDatabase.TABLE_NAME
                + " WHERE " + MmsSmsColumns.HASH + " = ?", new String[] {hash})
                .getCount() > 0 ? SMS_HASH : MMS_HASH;
    }
}
