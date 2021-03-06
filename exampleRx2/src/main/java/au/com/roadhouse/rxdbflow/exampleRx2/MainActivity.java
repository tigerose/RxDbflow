package au.com.roadhouse.rxdbflow.exampleRx2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.raizlabs.android.dbflow.sql.language.Method;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

import java.util.List;

import au.com.roadhouse.rxdbflow.rx2.DBFlowSchedulers;
import au.com.roadhouse.rxdbflow.exampleRx2.model.InheritanceTestModel;
import au.com.roadhouse.rxdbflow.exampleRx2.model.TestModel;
import au.com.roadhouse.rxdbflow.rx2.sql.language.NullValue;
import au.com.roadhouse.rxdbflow.rx2.sql.language.RxSQLite;
import au.com.roadhouse.rxdbflow.rx2.sql.transaction.RxGenericTransactionBlock;
import au.com.roadhouse.rxdbflow.rx2.sql.transaction.RxModelOperationTransaction;
import au.com.roadhouse.rxdbflow.rx2.structure.RxModelAdapter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private CompositeDisposable mDataSubscribers;
    private Disposable mDatabaseInitSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDataSubscribers = new CompositeDisposable();

        mDatabaseInitSubscription =
                RxSQLite.select(Method.count())
                        .from(TestModel.class)
                        .asCountSingle()
                        .restartOnChange()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<Long>() {
                                    @Override
                                    public void accept(Long aLong) throws Exception {
                                        Log.e("TEST", "call: I have been called");
                                        if (aLong == 0) {
                                            Log.e("TEST", "call: populating database");
                                            mDatabaseInitSubscription.dispose();
                                            populateDatabase();
                                        } else {
                                            Log.e("TEST", "call: clearing database");
                                            clearDatabase();
                                        }
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {

                                    }
                                },
                                new Action() {
                                    @Override
                                    public void run() throws Exception {
                                        Log.e("TEST", "completed");
                                    }
                                }

                        );
    }

    private void clearDatabase() {
        RxSQLite.delete(TestModel.class)
                .asExecuteCompletable()
                .notifyOfUpdates()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action() {
                            @Override
                            public void run() throws Exception {
                                Log.e("TEST", "TestModel table has been cleared");
                            }
                        });

        RxSQLite.delete(InheritanceTestModel.class)
                .asExecuteCompletable()
                .notifyOfUpdates()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action() {
                            @Override
                            public void run() throws Exception {
                                Log.e("TEST", "InheritanceTestModel table has been cleared");
                            }
                        }
                );
    }

    private void populateList() {

        //We are listening for table changes so we add the subscription to the datasubscribers
        //so the subscription can be cleaned up when it's no longer needed
        mDataSubscribers.add(
                RxSQLite.select()
                        .from(TestModel.class)
                        .asListSingle()
                        .restartOnChange(TestModel.class, InheritanceTestModel.class)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<List<TestModel>>() {
                            @Override
                            public void accept(List<TestModel> testModels) throws Exception {
                                Log.e("TEST", "We have " + testModels.size() + " test models ");
                            }
                        }));


        mDataSubscribers.add(
                RxSQLite.select()
                        .from(InheritanceTestModel.class)
                        .asListSingle()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<List<InheritanceTestModel>>() {
                            @Override
                            public void accept(List<InheritanceTestModel> testModels) throws Exception {
                                Log.e("TEST", "We have " + testModels.size() + " inheritance models ");
                            }

                        }));
    }

    private void populateDatabase() {
        insertWithModelOperations();
        insertWithGenericTransactionBlock();
        insertWithModelOperationTransaction();

        RxSQLite.select().from(TestModel.class).asListSingle().toObservable().subscribe();
    }

    private void insertWithModelOperations() {
        TestModel modelOne = new TestModel();
        modelOne.setFirstName("Bob");
        modelOne.setLastName("Marley");

        TestModel modelTwo = new TestModel();
        modelOne.setFirstName("John");
        modelOne.setLastName("Doe");

        TestModel modelThree = new TestModel();
        modelOne.setFirstName("Bob");
        modelOne.setLastName("Don");

        RxModelAdapter.getModelAdapter(TestModel.class)
                .insertAsSingle(modelOne)
                .subscribeOn(DBFlowSchedulers.background())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<TestModel>() {
                    @Override
                    public void accept(TestModel testModel) throws Exception {
                        Log.e("TEST", "Inserting model one");
                    }
                });


        RxModelAdapter.getModelAdapter(TestModel.class)
                .insertAsSingle(modelTwo)
                .subscribeOn(DBFlowSchedulers.background())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<TestModel>() {
                    @Override
                    public void accept(TestModel testModel) {
                        Log.e("TEST", "Inserting model two");
                    }
                });


        RxModelAdapter.getModelAdapter(TestModel.class)
                .insertAsSingle(modelThree)
                .subscribeOn(DBFlowSchedulers.background())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<TestModel>() {
                    @Override
                    public void accept(TestModel testModel) {
                        Log.e("TEST", "Inserting model three");
                    }
                });


    }

    private void insertWithModelOperationTransaction() {
        //Inheritance insertions
        InheritanceTestModel modelOne = new InheritanceTestModel();
        modelOne.setFirstName("Bob");
        modelOne.setLastName("Marley");

        InheritanceTestModel modelTwo = new InheritanceTestModel();
        modelOne.setFirstName("John");
        modelOne.setLastName("Doe");

        InheritanceTestModel modelThree = new InheritanceTestModel();
        modelOne.setFirstName("Bob");
        modelOne.setLastName("Don");

        mDataSubscribers.add(new RxModelOperationTransaction.Builder(InheritanceTestModel.class)
                .setDefaultOperation(RxModelOperationTransaction.MODEL_OPERATION_INSERT)
                .addModel(modelOne)
                .addModel(modelTwo)
                .addModel(modelThree)
                .build()
                .subscribeOn(DBFlowSchedulers.background())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<NullValue>() {
                    @Override
                    public void accept(NullValue aVoid) {
                        Log.e("TEST", "Finished inserting modelOperation models ");
                    }
                }));

        TestModel testModelOne = new TestModel();
        testModelOne.setFirstName("Bob");
        testModelOne.setLastName("Marley");

        TestModel testModelTwo = new TestModel();
        testModelOne.setFirstName("John");
        testModelOne.setLastName("Doe");

        TestModel testModelThree = new TestModel();
        testModelOne.setFirstName("Bob");
        testModelOne.setLastName("Don");

        mDataSubscribers.add(new RxModelOperationTransaction.Builder(InheritanceTestModel.class)
                .setDefaultOperation(RxModelOperationTransaction.MODEL_OPERATION_INSERT)
                .addModel(testModelOne)
                .addModel(testModelTwo)
                .addModel(testModelThree)
                .build()
                .subscribeOn(DBFlowSchedulers.background())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<NullValue>() {
                    @Override
                    public void accept(NullValue aVoid) {
                        Log.e("TEST", "Finished inserting modelOperation models ");
                    }
                }));
    }

    private void insertWithGenericTransactionBlock() {
        //GenericTransactionBlock example
        mDataSubscribers.add(
                new RxGenericTransactionBlock.Builder(InheritanceTestModel.class)
                        .addOperation(new RxGenericTransactionBlock.TransactionOperation() {
                            @Override
                            public boolean onProcess(DatabaseWrapper databaseWrapper) {
                                InheritanceTestModel test = new InheritanceTestModel();
                                test.setFirstName("Bob");
                                test.setLastName("Marley");
                                test.insert();
                                return true;
                            }
                        })
                        .addOperation(new RxGenericTransactionBlock.TransactionOperation() {
                            @Override
                            public boolean onProcess(DatabaseWrapper databaseWrapper) {
                                InheritanceTestModel test = new InheritanceTestModel();
                                test.setFirstName("John");
                                test.setLastName("Doe");
                                test.insert();

                                return true;
                            }
                        })
                        .addOperation(new RxGenericTransactionBlock.TransactionOperation() {
                            @Override
                            public boolean onProcess(DatabaseWrapper databaseWrapper) {
                                InheritanceTestModel test = new InheritanceTestModel();
                                test.setFirstName("Elvis");
                                test.setLastName("Presely");
                                test.insert();

                                return true;
                            }
                        }).build()
                        .subscribeOn(DBFlowSchedulers.background())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<NullValue>() {
                            @Override
                            public void accept(NullValue aVoid) {
                                Log.e("TEST", "Finished inserting genericTransactionBlock models ");
                                populateList();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                Log.e("TEST", "call: Transaction error occurred", throwable);
                            }
                        }));
    }

    @Override
    protected void onDestroy() {
        //Clean up all data subscribes
        mDataSubscribers.dispose();
        SQLite.delete().from(TestModel.class).execute();
        SQLite.delete().from(InheritanceTestModel.class).execute();
        super.onDestroy();
    }
}