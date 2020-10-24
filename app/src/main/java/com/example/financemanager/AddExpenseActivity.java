package com.example.financemanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.loader.content.AsyncTaskLoader;

import android.content.Loader;
import android.app.LoaderManager;
import android.content.CursorLoader;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.financemanager.ExpenditureDatabaseContract.ExpenditureInfoEntry;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class AddExpenseActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXPENDITURE_ID = "com.example.financemanager.EXPENDITURE_ID";
    public static final int ID_NOT_SET = -1;
    public static final int LOADER_EXPENSE = 0;
    private final String TAG = getClass().getSimpleName();
    private int mExpenditureId;
    private boolean mIsNewExpense;
    private ExpenditureOpenHelper mDbOpenHelper;
    private TextView mHeader;
    private Cursor mExpenseCursor;
    private int mExpenseIdPosition;
    private int mExpenseNamePosition;
    private int mExpenseAmountPosition;
    private int mExpenditureDescriptionPosition;
    private EditText mExpNameEditText;
    private EditText mExpAmountEditText;
    private EditText mExpDescriptionEditText;
    private Spinner mSpinner;
    private ArrayAdapter<String> mArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        mDbOpenHelper = new ExpenditureOpenHelper(this);
        mHeader = (TextView) findViewById(R.id.textView);
        mExpNameEditText = (EditText) findViewById(R.id.editTextTextPersonName);
        mExpAmountEditText = (EditText) findViewById(R.id.editTextTextNumber);
        mExpDescriptionEditText = (EditText) findViewById(R.id.editTextTextMultiLine);

        Window window = AddExpenseActivity.this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorAccent));

        final Button button = (Button) findViewById(R.id.button_add_expense);
        ConstraintLayout.LayoutParams buttonParams = (ConstraintLayout.LayoutParams) button.getLayoutParams();
        buttonParams.height = (int) Math.round(getScreenHeight() * 0.1);
        button.setLayoutParams(buttonParams);
        button.post(new Runnable() {
            @Override
            public void run() {
                Drawable image = getApplicationContext().getResources().getDrawable(R.drawable.ic_forward);
                image.setBounds(0, 0, getIconSize() * 2, getIconSize());
                button.setCompoundDrawables(null, null, image, null);
            }
        });

        mSpinner = (Spinner) findViewById(R.id.spinner_category);
        String[] Categories = new String[]{"Food", "Housing",
        "Fashion", "Education", "Entertainment", "Transportation",
        "Investment", "Technology", "Recreation", "Others"};

        final List<String> categoryList = new ArrayList<>(Arrays.asList(Categories));
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mArrayAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String category = parent.getItemAtPosition(position).toString();
                Toast.makeText(parent.getContext(), "Category " + category, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        readDisplayStateValues();

        if(!mIsNewExpense)
            getLoaderManager().initLoader(LOADER_EXPENSE, null, this);

    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();

        // get the note id passes from the list of notes
        mExpenditureId = intent.getIntExtra(EXPENDITURE_ID, ID_NOT_SET);
        mIsNewExpense = this.mExpenditureId == ID_NOT_SET;
        if (mIsNewExpense) {
            mHeader.setText("Add Expense");
            createNewExpense();
        } else {
            mHeader.setText("Edit Expense");
        }

        Log.i("Position", "Expense Position: " + mExpenditureId);
        //mNote = DataManager.getInstance().getNotes().get(this.mNoteId);
    }

    private void displayExpense() {
        String expenseId = mExpenseCursor.getString(mExpenseIdPosition);
        String expenseName = mExpenseCursor.getString(mExpenseNamePosition);
        String expenseAmount = Integer.toString(mExpenseCursor.getInt(mExpenseAmountPosition));
        String expenseDescription = mExpenseCursor.getString(mExpenditureDescriptionPosition);
        mExpNameEditText.setText(expenseName);
        mExpAmountEditText.setText(expenseAmount);
        mExpDescriptionEditText.setText(expenseDescription);
        String formattedExpenseId = capitalize(expenseId);
        Log.i("Expense", "Expense id " + formattedExpenseId);
        int coursePosition = mArrayAdapter.getPosition(formattedExpenseId);
        mSpinner.setSelection(coursePosition);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbOpenHelper.close();
    }

    private String capitalize(String str) {
        if (str.isEmpty() || str == null) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String deCapitalize(String str) {
        if(str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private void createNewExpense() {
        final ContentValues values = new ContentValues();
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_NAME, "");
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_AMOUNT, 0);
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_DESCRIPTION, "");
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_ID, "");
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_DAY, "");
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_MONTH, "");
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_YEAR, "");

        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
                mExpenditureId = (int) db.insert(ExpenditureInfoEntry.TABLE_NAME, null, values);
                return null;
            }
        };
        task.execute();

        mExpAmountEditText.setText("0");
        Log.i("Expenditure", "New Expense at position " + mExpenditureId);
    }

    private static int getIconSize() {
        return (int) Math.round(getScreenHeight() * 0.05);
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }


    public void saveExpense(View view) {
        String expenseName = mExpNameEditText.getText().toString();
        int expenseAmount = Integer.parseInt(mExpAmountEditText.getText().toString());
        String expenseDescription = mExpDescriptionEditText.getText().toString();
        String expenseId = deCapitalize(mSpinner.getSelectedItem().toString());
        Log.i("Expenditure", " New Expense Id " + expenseId);

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        String monthName = getMonthFromInt(month);
        int year = calendar.get(Calendar.YEAR);

        Log.i("Expenditure", "Today's Date " + day + " " +
                monthName + " " + year);
        saveExpenseToDatabase(expenseName, expenseAmount, expenseDescription, expenseId, day, monthName, year);
    }

    private void saveExpenseToDatabase(String expenseName, int expenseAmount, String expenseDescription,
                                       String expenseId, int day, String monthName, int year) {

        final String selection = ExpenditureInfoEntry._ID + " = ?";
        final String[] selectionArgs = new String[]{Integer.toString(mExpenditureId)};

        final ContentValues values = new ContentValues();
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_NAME, expenseName);
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_AMOUNT, expenseAmount);
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_DESCRIPTION, expenseDescription);
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_ID, expenseId);
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_DAY, Integer.toString(day));
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_MONTH, monthName);
        values.put(ExpenditureInfoEntry.COLUMN_EXPENDITURE_YEAR, Integer.toString(year));

        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
                db.update(ExpenditureInfoEntry.TABLE_NAME,values, selection, selectionArgs);
                return null;
            }
        };
        task.execute();

        finish();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancelChanges();
    }

    public void cancel(View view) {
        cancelChanges();
    }

    private void cancelChanges() {
        if(mIsNewExpense) {
            SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
            String selection = ExpenditureInfoEntry._ID + " = ?";
            String[] selectionArgs = new String[]{Integer.toString(mExpenditureId)};
            db.delete(ExpenditureInfoEntry.TABLE_NAME, selection, selectionArgs);
        }
        finish();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        if (id == LOADER_EXPENSE)
            loader = createLoaderExpense();
        return loader;
    }

    private CursorLoader createLoaderExpense() {
        return new CursorLoader(this) {
            @Override
            public Cursor loadInBackground() {
                SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

                String selection = ExpenditureInfoEntry._ID + " = ?";
                String[] selectionArgs = {Integer.toString(mExpenditureId)};

                String[] expenseColumns = {
                        ExpenditureInfoEntry.COLUMN_EXPENDITURE_NAME,
                        ExpenditureInfoEntry.COLUMN_EXPENDITURE_AMOUNT,
                        ExpenditureInfoEntry.COLUMN_EXPENDITURE_ID,
                        ExpenditureInfoEntry.COLUMN_EXPENDITURE_DESCRIPTION
                };

                return db.query(ExpenditureInfoEntry.TABLE_NAME,
                        expenseColumns, selection, selectionArgs, null, null, null);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_EXPENSE) {
            mExpenseCursor = data;
            mExpenseIdPosition = mExpenseCursor.getColumnIndex(ExpenditureInfoEntry.COLUMN_EXPENDITURE_ID);
            mExpenseNamePosition = mExpenseCursor.getColumnIndex(ExpenditureInfoEntry.COLUMN_EXPENDITURE_NAME);
            mExpenseAmountPosition = mExpenseCursor.getColumnIndex(ExpenditureInfoEntry.COLUMN_EXPENDITURE_AMOUNT);
            mExpenditureDescriptionPosition = mExpenseCursor.getColumnIndex(ExpenditureInfoEntry.COLUMN_EXPENDITURE_DESCRIPTION);
            mExpenseCursor.moveToNext();
            displayExpense();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_EXPENSE) {
            if(mExpenseCursor != null)
                mExpenseCursor.close();
        }
    }
}