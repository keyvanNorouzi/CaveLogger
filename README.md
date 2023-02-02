# CaveLogger
CaveLogger is In-App logger for Http requests

First add jitpack.io in your gradle

1.
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
	
After that add CaveLogger Library
  
2. dependencies {
	        implementation 'com.github.keyvanNorouzi:CaveLogger:alpha01'
	}


Add 'CaveLogger' with koin 

// Provide CaveLoggingInterceptor
    single(StringQualifier(CAVE_INTERCEPTOR)) {
        val logging = CaveInterceptor(get())
        if (BuildConfig.DEBUG) {
            logging.level = CaveInterceptor.Level.BODY
        } else {
            logging.level = CaveInterceptor.Level.NONE
        }
        logging
    }

after in your Retrofit module 
    .addInterceptor(get<CaveInterceptor>(StringQualifier(CAVE_INTERCEPTOR)))

JUST THIS !

For see your HTTP Request just open 'CaveLoggerActivity'
