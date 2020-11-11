package com.example.financemanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Loader;
import android.app.LoaderManager;
import android.content.CursorLoader;
//import androidx.loader.app.LoaderManager;
//import androidx.loader.content.Loader;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.financemanager.FinanceManagerDatabaseContract.IncomeInfoEntry;
import com.example.financemanager.FinanceManagerProviderContract.Incomes;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public class NetIncomeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mRecyclerIncome;
    private static final int LOADER_INCOMES = 0;
    private LinearLayoutManager mIncomeLayoutManager;
    private IncomeRecyclerAdapter mIncomeRecyclerAdapter;
    private FinanceManagerOpenHelper mDbOpenHelper;
    private Cursor mIncomeCursor;
    private EditText mIncomeAmountInputField;
    private ConstraintLayout mParent;
    private String mAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_income);

        mDbOpenHelper = new FinanceManagerOpenHelper(this);
        mIncomeAmountInputField = (EditText) findViewById(R.id.editTextNumber_income_amount);
        mParent = (ConstraintLayout) findViewById(R.id.parent_net_income);

        initializeDisplayContent();
        getLoaderManager().restartLoader(LOADER_INCOMES, null, this);
    }

    private void initializeDisplayContent() {
        mRecyclerIncome = findViewById(R.id.list_income);
        mIncomeLayoutManager = new LinearLayoutManager(this);

        mIncomeRecyclerAdapter = new IncomeRecyclerAdapter(this, null);
        displayIncomes();
    }

    private void displayIncomes() {
        mRecyclerIncome.setLayoutManager(mIncomeLayoutManager);
        mRecyclerIncome.setAdapter(mIncomeRecyclerAdapter);

    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        CursorLoader loader = null;
        if (id == LOADER_INCOMES) {
            loader = createLoaderIncome();
        }
        return loader;
    }

    private CursorLoader createLoaderIncome() {
        Uri uri = Incomes.CONTENT_URI;
        String[] columns = {
                Incomes.COLUMN_INCOME_AMOUNT,
                Incomes.COLUMN_INCOME_DAY,
                Incomes.COLUMN_INCOME_MONTH,
                Incomes.COLUMN_INCOME_YEAR
        };
        return new CursorLoader(this, uri, columns, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_INCOMES) {
            mIncomeCursor = data;
            mIncomeRecyclerAdapter.changeCursor(mIncomeCursor);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (loader.getId() == LOADER_INCOMES) {
            if (mIncomeCursor != null)
                mIncomeCursor.close();
        }
    }

    public void saveIncome(View view) {
        // get income amount to be stored
        mAmount = mIncomeAmountInputField.getText().toString();
        if (!mAmount.isEmpty()) {
            // get current date to be stored
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH);
            String monthName = getMonthFromInt(month);
            int year = calendar.get(Calendar.YEAR);

            final ContentValues values = new ContentValues();
            values.put(IncomeInfoEntry.COLUMN_INCOME_AMOUNT, mAmount);
            values.put(IncomeInfoEntry.COLUMN_INCOME_DAY, Integer.toString(day));
            values.put(IncomeInfoEntry.COLUMN_INCOME_MONTH, monthName);
            values.put(IncomeInfoEntry.COLUMN_INCOME_YEAR, Integer.toString(year));

            AsyncTask task = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
                    db.insert(IncomeInfoEntry.TABLE_NAME, null, values);
                    Snackbar.make(mParent, "Saved Succesfully.", Snackbar.LENGTH_SHORT).show();
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    getLoaderManager().restartLoader(LOADER_INCOMES, null, NetIncomeActivity.this);
                    Snackbar.make(mParent, "Saved", Snackbar.LENGTH_SHORT).show();
                    super.onPostExecute(o);
                }
            };
            task.execute();
            updateAmount();
        } else {
            Snackbar.make(mParent, "Enter a valid amount", Snackbar.LENGTH_SHORT).show();
        }

    }

    private double getOriginalAmount() {
        String[] columns = {FinanceManagerDatabaseContract.AmountInfoEntry.COLUMN_AMOUNT};
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(FinanceManagerDatabaseContract.AmountInfoEntry.TABLE_NAME, columns,null,null,
                null,null,null);
        cursor.moveToFirst();
        int amountPos = cursor.getColumnIndex(FinanceManagerDatabaseContract.AmountInfoEntry.COLUMN_AMOUNT);
        String amount = cursor.getString(amountPos);
        double amountDouble = Double.parseDouble(amount);
        return amountDouble;
    }

    private void updateAmount() {
        double originalAmountInDatabase = getOriginalAmount();
        double newAmountTobeAdded = Double.parseDouble(mAmount);
        double newAmountForDatabase = originalAmountInDatabase + newAmountTobeAdded;
        if (newAmountForDatabase < 0)
            newAmountForDatabase = 0;
        final String selection = FinanceManagerDatabaseContract.AmountInfoEntry.COLUMN_AMOUNT + " = ?";
        final String[] selectionArgs = {Double.toString(originalAmountInDatabase)};
        final ContentValues values = new ContentValues();
        values.put(FinanceManagerDatabaseContract.AmountInfoEntry.COLUMN_AMOUNT, newAmountForDatabase);
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
                db.update(FinanceManagerDatabaseContract.AmountInfoEntry.TABLE_NAME, values, selection, selectionArgs);
                return null;
            }
        };
        task.execute();
    }



    private String getMonthFromInt(int month) {
        String monthString = "";
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        if (month >= 0 && month <= 11) {
            monthString = months[month];
        }
        return monthString;
    }
}