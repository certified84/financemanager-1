package com.example.financemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.financemanager.ExpenditureDatabaseContract.AmountInfoEntry;
import com.example.financemanager.ExpenditureDatabaseContract.ExpenditureInfoEntry;
import com.example.financemanager.ExpenditureDatabaseContract.IncomeInfoEntry;

public class ExpenditureOpenHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "FinanceManager.db";
    public static final int DATABASE_VERSION = 1;

    public ExpenditureOpenHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ExpenditureInfoEntry.SQL_CREATE_TABLE);
        db.execSQL(ExpenditureInfoEntry.SQL_CREATE_INDEX1);
        db.execSQL(IncomeInfoEntry.SQL_CREATE_TABLE);
        db.execSQL(IncomeInfoEntry.SQL_CREATE_INDEX1);
        db.execSQL(AmountInfoEntry.SQL_CREATE_TABLE);

        DatabaseDataWorker worker = new DatabaseDataWorker(db);
//        worker.insertSampleExpenditures();
        worker.insertSampleIncome();
        worker.insertAmount();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
