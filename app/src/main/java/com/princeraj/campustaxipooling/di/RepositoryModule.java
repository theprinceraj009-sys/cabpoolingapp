package com.princeraj.campustaxipooling.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.princeraj.campustaxipooling.db.CampusTaxiDatabase;
import com.princeraj.campustaxipooling.repository.ChatRepositoryImpl;
import com.princeraj.campustaxipooling.repository.IChatRepository;
import com.princeraj.campustaxipooling.repository.IReportRepository;
import com.princeraj.campustaxipooling.repository.IRideRepository;
import com.princeraj.campustaxipooling.repository.IUserRepository;
import com.princeraj.campustaxipooling.repository.ReportRepositoryImpl;
import com.princeraj.campustaxipooling.repository.RideRepositoryImpl;
import com.princeraj.campustaxipooling.repository.UserRepositoryImpl;
import com.princeraj.campustaxipooling.sync.SyncManager;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Repository dependency injection module.
 * Provides singleton implementations for all repository interfaces.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    @Singleton
    public abstract IRideRepository bindRideRepository(RideRepositoryImpl impl);

    // We can also use @Provides for implementations that have complex constructor logic
    // but RideRepositoryImpl now uses @Inject constructor, so @Binds is better.

    @Binds
    @Singleton
    public abstract IUserRepository bindUserRepository(UserRepositoryImpl impl);

    @Provides
    @Singleton
    public static IChatRepository provideChatRepository(
            FirebaseFirestore db, 
            CampusTaxiDatabase database, 
            com.princeraj.campustaxipooling.util.AppExecutors executors) {
        return new ChatRepositoryImpl(db, database, executors);
    }

    @Provides
    @Singleton
    public static IReportRepository provideReportRepository(FirebaseFirestore db) {
        return new ReportRepositoryImpl(db);
    }

    @Provides
    @Singleton
    public static com.princeraj.campustaxipooling.repository.IAdminRepository provideAdminRepository(FirebaseFirestore db) {
        return new com.princeraj.campustaxipooling.repository.AdminRepositoryImpl(db);
    }
}
