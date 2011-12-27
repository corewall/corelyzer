#include "corelyzer_plugin_correlator_Correlator.h"

#include <iostream>
#include "DataManager.h"
#include "CullFilter.h"
#include "DecimateFilter.h"
#include "GaussianFilter.h"
#include "Correlater.h"
#include "AutoCorrelater.h"
#include "Section.h"

using namespace std;

// global stuff
DataManager* manager   = NULL;
Data* dataptr          = NULL;
Correlater* correlator = NULL;
Hole* splicedataptr    = NULL;

//************************** JNI FUNCTIONS ********************************//
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_plugin_correlator_Correlator_init
  (JNIEnv * jenv, jclass jclass)
{
    // some init code if necessary
    std::cout << "Hello Correlator!" << std::endl;
    manager = new DataManager();

    return true;            
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    finish
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_finish
  (JNIEnv * jenv, jclass jclass)
{
    if(dataptr)
    {
        delete dataptr;
        dataptr = NULL;
    }  

    if(manager)
    {
        delete manager;
        manager = NULL;
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    setCoreFormat
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_setCoreFormat
  (JNIEnv * jenv, jclass jclass, jint jFormatIndex)
{
    // jFormatIndex starts from 0, reference: Correlator#CoreType.h

	// formats
	// MST95REPORT, TKREPORT, ODPOTHER, JANUSORIG
	manager->setCoreFormat(jFormatIndex);
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getCoreFormat
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_corelyzer_plugin_correlator_Correlator_getCoreFormat
  (JNIEnv * jenv, jclass jclass)
{
    return manager->getCoreFormat();
}


/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    setCoreType
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_setCoreType
  (JNIEnv * jenv, jclass jclass, jint jTypeIndex)
{
    // jTypeIndex starts from 30, reference: Correlator#CoreType.h
    //  type
    // GRAPE, PWAVE, SUSCEPTIBILITY, NATURALGAMMA, REFLECTANCE, OTHERTYPE
    int index = 30 + jTypeIndex;
    manager->setCoreType(index);
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getCoreType
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_corelyzer_plugin_correlator_Correlator_getCoreType__
  (JNIEnv * jenv, jclass jclass)
{
    return manager->getCoreType();
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    loadAffineTable
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_loadAffineTable
  (JNIEnv * jenv, jclass jclass, jstring jPath)
{
    int i = jenv->GetStringLength(jPath);
    if(i <= 1) return;

    char* nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(jPath, 0, i, nbuf);

    dataptr = manager->load(nbuf, dataptr);
    
    free(nbuf);
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    loadSpliceTable
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_loadSpliceTable
  (JNIEnv * jenv, jclass jclass, jstring jPath)
{
    int i = jenv->GetStringLength(jPath);
    if(i <= 1) return;

    char* nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(jPath, 0, i, nbuf);

    dataptr= manager->load(nbuf, dataptr);

    free(nbuf);
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    loadData
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_loadData
  (JNIEnv * jenv, jclass jclass, jstring jPath)
{

    int i = jenv->GetStringLength(jPath);
    if(i <= 1) return;

    char* nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(jPath, 0, i, nbuf);

    dataptr = manager->load(nbuf, dataptr);

    free(nbuf);
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    updateData
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_updateData
  (JNIEnv * jenv, jclass jclass)
{
    if(dataptr != NULL)
    {
        dataptr->update();
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    execute
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_execute
  (JNIEnv * jenv, jclass jclass, jboolean hasSplice)
{
    correlator = new Correlater(dataptr);
    correlator->generateSpliceHole();

    if(hasSplice)
    {
        splicedataptr = correlator->getSpliceHole();
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getHoleNames
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_corelyzer_plugin_correlator_Correlator_getHoleNames
  (JNIEnv * jenv, jclass jcls)
{
    if(dataptr)
    {
		int holesize = dataptr->getNumOfHoles();

        // prepare return array
        jstring str = NULL;
        jclass strCls = jenv->FindClass("java/lang/String");
        jobjectArray strArray = jenv->NewObjectArray(holesize, strCls, NULL);

		Hole* holeptr = NULL;
		for(int i=0; i < holesize; i++)
		{
		    holeptr  = dataptr->getHole(i);

            char* holeName = new char(2);
            holeName[0] = holeptr->getName(); // ? just single char?
            holeName[1] = '\0';
            str = jenv->NewStringUTF(holeName);
            jenv->SetObjectArrayElement(strArray, i, str);
            jenv->DeleteLocalRef(str);
        }

        return strArray;                    
    }
    else
    {
        return NULL;
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getCoreNames
 * Signature: (I)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_corelyzer_plugin_correlator_Correlator_getCoreNames
  (JNIEnv * jenv, jclass jcls, jint jholeIdx)
{
    if(dataptr)
    {
		int holesize = dataptr->getNumOfHoles();
        if( (jholeIdx < 0) || (jholeIdx >= holesize) ) return NULL;

        Hole* holeptr = dataptr->getHole((int) jholeIdx);
        int coreSize = holeptr->getNumOfCores();

        cout << "---> [INFO] holeIdx " << jholeIdx << ", " << holeptr->getName()
             << " has " << coreSize << " cores." << endl;
        
        // prepare return array
        jstring str = NULL;
        jclass strCls = jenv->FindClass("java/lang/String");
        jobjectArray strArray = jenv->NewObjectArray(coreSize, strCls, NULL);

        Core* corePtr = NULL;

        cout << "start looping cores" << endl;
        for(int i=0; i<coreSize; i++)
        {
            corePtr = holeptr->getCore(i);

            if(corePtr != NULL)
            {
                char coreName[5];
                sprintf(coreName, "%d", corePtr->getNumber());
                
                str = jenv->NewStringUTF(coreName);
                jenv->SetObjectArrayElement(strArray, i, str);
                jenv->DeleteLocalRef(str);            
            }
            else
            {
                cout << "---> [WARN] NULL corePtr , continue" << endl;
            }
        }

        return strArray;
    }
    else
    {
        return NULL;
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getSectionNames
 * Signature: (II)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_corelyzer_plugin_correlator_Correlator_getSectionNames
  (JNIEnv * jenv, jclass jcls, jint jholeIdx, jint jcoreIdx)
{
    if(dataptr)
    {
		int holesize = dataptr->getNumOfHoles();
        if( (jholeIdx < 0) || (jholeIdx >= holesize) ) return NULL;

        Hole* holeptr = dataptr->getHole((int) jholeIdx);
        int coreSize = holeptr->getNumOfCores();

        if( (jcoreIdx < 0) || (jcoreIdx >= coreSize) ) return NULL;
        Core* coreptr = holeptr->getCore(jcoreIdx);
        int sectionSize = coreptr->getNumOfSections();

        cout << "Core " << coreptr->getNumber() << " has " << sectionSize << " sections." << endl;

        // prepare return array
        jstring str = NULL;
        jclass strCls = jenv->FindClass("java/lang/String");
        jobjectArray strArray = jenv->NewObjectArray(sectionSize, strCls, NULL);

        Section* sectionptr = NULL;
        for(int i=0; i<sectionSize; ++i)
        {
            sectionptr = coreptr->getSection(i+1); // FIXME

            if(sectionptr != NULL)
            {
                char sectionName[5];
                sprintf(sectionName, "%d", sectionptr->getNumber());

                str = jenv->NewStringUTF(sectionName);
                jenv->SetObjectArrayElement(strArray, i, str);
                jenv->DeleteLocalRef(str);
            }
            else
            {
                cout << "---> [WARN] NULL sectionptr, continue" << endl;
            }
        }

        return strArray;
    }
    else
    {
        return NULL;    
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getLeg
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_plugin_correlator_Correlator_getLeg
  (JNIEnv * jenv, jclass jcls, jint jholeIdx)
{
    if(dataptr)
    {
        int holeSize = dataptr->getNumOfHoles();

        if( (jholeIdx < 0) || (jholeIdx >= holeSize) )
        {
            return -1;
        }

        Hole* holeptr = dataptr->getHole((int)jholeIdx);
        if(holeptr)
        {
            return holeptr->getLeg();
        }
        else
        {
            return -1;
        }        
    }
    else
    {
        return -1;
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getSite
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_plugin_correlator_Correlator_getSite
  (JNIEnv * jenv, jclass jcls, jint jholeIdx)
{
    if(dataptr)
    {
        int holeSize = dataptr->getNumOfHoles();

        if( (jholeIdx < 0) || (jholeIdx >= holeSize) )
        {
            return -1;
        }

        Hole* holeptr = dataptr->getHole((int)jholeIdx);
        if(holeptr)
        {
            return holeptr->getSite();
        }
        else
        {
            return -1;
        }
    }
    else
    {
        return -1;
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getCoreType
 * Signature: (II)C
 */
JNIEXPORT jchar JNICALL Java_corelyzer_plugin_correlator_Correlator_getCoreType__II
  (JNIEnv * jenv, jclass jcls, jint jholeIdx, jint jcoreIdx)
{
    if(dataptr)
    {
        int holeSize = dataptr->getNumOfHoles();

        if( (jholeIdx < 0) || (jholeIdx >= holeSize) )
        {
            return NULL;
        }

        Hole* holeptr = dataptr->getHole((int)jholeIdx);
        if(holeptr)
        {
            int coreSize = holeptr->getNumOfCores();

            if( (jcoreIdx >= 0) && (jcoreIdx < coreSize) )
            {
                Core* coreptr = holeptr->getCore((int)jcoreIdx);

                if(coreptr != NULL)
                {
                    return coreptr->getCoreType();
                }
                else
                {
                    return NULL;
                }
            }
            else
            {
                return NULL;
            }
        }
        else
        {
            return NULL;
        }
    }
    else
    {
        return NULL;
    }
}


/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getHoleData
 * Signature: (I)[F
 */
JNIEXPORT jfloatArray JNICALL Java_corelyzer_plugin_correlator_Correlator_getHoleData
  (JNIEnv * jenv, jclass jcls, jint holeIdx)
{
    if(dataptr)
    {
        Hole* holeptr = dataptr->getHole((int) holeIdx);
		int coresize = holeptr->getNumOfCores();

		Core* coreptr = NULL;
		Value* valueptr = NULL;
				
        std::cout << "Working on hole " << holeptr->getName() << " with "
                  << coresize << " cores" << std::endl;

        int count = 0;
        for(int j=0; j < coresize; j++)
        {
            coreptr = holeptr->getCore(j);
            if(coreptr == NULL)
            {
                continue;
            }

            data_range* range = coreptr->getRange();
            int valuesize = coreptr->getNumOfValues();

            for(int k=0; k < valuesize; k++)
            {
                valueptr = coreptr->getValue(k);
                if(valueptr == NULL) continue;

                count++;
            }
        }

        cout << "Have " << count << " sensoring samples" << endl;
        float vArray[2*count];
        // for(int i = 0; i<count; ++i)
        // {
        //     vArray[i] = 0.0f;
        // }
        jfloatArray tuples = jenv->NewFloatArray(2*count);

        int idx = 0;
		for(int j=0; j < coresize; j++)
		{
		    coreptr = holeptr->getCore(j);
		    if (coreptr == NULL)
		    {
		        continue;
            }

            data_range* range = coreptr->getRange();
            int valuesize = coreptr->getNumOfValues();

            // cout << "Working on core " << coreptr->getName() << " [" << valuesize << "]" << endl;
            for(int k=0; k < valuesize; k++)
            {
                valueptr = coreptr->getValue(k);
                if (valueptr == NULL) continue;

                // cout << "Tuple: [Mcd: " << valueptr->getMcd() << ", Value: " << valueptr->getData() << "]" << endl;
                vArray[idx]   = valueptr->getMcd();
                vArray[idx+1] = valueptr->getData();
                idx += 2;

                // valueptr->getMbsf();
                // valueptr->getMcd();

                // this function gave smoothed data if it's smoothed.
                // if not, then raw data
                // valueptr->getData();

                // However, you need raw data even it it's smoothed, then use this
                // valueptr->getRawData();

            } // end of for-value
		} // end of for-core

        // printf("[C] 1st:  depth0:value0 = %f, %f\n", vArray[0], vArray[1]);
        // printf("[C] last: depthN:valueN = %f, %f\n", vArray[2*count-2], vArray[2*count-1]);

        jenv->SetFloatArrayRegion(tuples, 0, 2*count, vArray);

        // jenv->ReleaseFloatArrayElements(tuples, vArray, 0);

        return tuples;		                
    }
    else
    {
        return NULL;
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getDataTuple
 * Signature: (III)[F
 */
/*
JNIEXPORT jfloatArray JNICALL Java_corelyzer_plugin_correlator_Correlator_getDataTuple
  (JNIEnv * jenv, jclass jclass, jint holeIdx, jint sectionIdx, jint tupleIdx)
{

}
*/

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    debugPrintOut
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_debugPrintOut
  (JNIEnv * jenv, jclass jclass)
{
    std::cout << "[Correlator] Debug print out information" << std::endl;
    if(dataptr)
    {
        dataptr->debugPrintOut();
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    printoutData
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_corelyzer_plugin_correlator_Correlator_printoutData
  (JNIEnv * jenv, jclass jclass)
{
	if(dataptr)
	{
		// how to get core data (depth, data)
		// how to get affine, core range etc...
		int holesize = dataptr->getNumOfHoles();
		int coresize, valuesize;

		data_range* range;
		Hole* holeptr = NULL;
		Core* coreptr = NULL;
		Value* valueptr = NULL;

        std::cout << "Loop through " << holesize << " holes." << std::endl;
		for(int i=0; i < holesize; i++)
		{
		    holeptr = dataptr->getHole(i);
		    coresize = holeptr->getNumOfCores();

            std::cout << "Working on hole " << holeptr->getName() << " [" << coresize << "]" << std::endl;
		    for(int j=0; j < coresize; j++)
		    {
				coreptr = holeptr->getCore(j);
				if (coreptr == NULL) {
                    cout << "skip" << endl;
				    continue;
                }
				// offset  : from affine
		        // coreptr->getDepthOffset();

		        range = coreptr->getRange();
				// data_range structure
				// top, bottom, mindepth, maxdepth,maxdepthdiff, avedepstep
				// min, max, --> data
				// realminvar, realmaxvar, --> data
				// ratio // mcd/mfcd

				// in case for splice data, you can get extra information where the data is from
				// coreptr->getAnnotation();
				valuesize = coreptr->getNumOfValues();

                std::cout << "Working on core " << coreptr->getName() << " [" << valuesize << "]" << std::endl;
		        for(int k=0; k < valuesize; k++)
				{
			    	valueptr = coreptr->getValue(k);
					if (valueptr == NULL) continue;

					std::cout << "Tuple: [Mcd: " << valueptr->getMcd() << ", Value: " << valueptr->getData() << "]" << std::endl;
					
			    	// valueptr->getMbsf();
			    	// valueptr->getMcd();

			    	// this function gave smoothed data if it's smoothed.
			    	// if not, then raw data
			    	// valueptr->getData();

			    	// However, you need raw data even it it's smoothed, then use this
			    	// valueptr->getRawData();

				} // end of for-value
		    } // end of for-core
		}// end of for-hole
    }
}

/*
 * Class:     corelyzer_plugin_correlator_Correlator
 * Method:    getSectionInterval
 * Signature: (III)[F
 */
JNIEXPORT jfloatArray JNICALL Java_corelyzer_plugin_correlator_Correlator_getSectionInterval
  (JNIEnv * jenv, jclass jcls, jint jholeIdx, jint jcoreIdx, jint jsectionIdx)
{
    if(dataptr)
    {
		int holesize = dataptr->getNumOfHoles();
        if( (jholeIdx < 0) || (jholeIdx >= holesize) ) return NULL;

        Hole* holeptr = dataptr->getHole((int) jholeIdx);
        int coreSize = holeptr->getNumOfCores();

        if( (jcoreIdx < 0) || (jcoreIdx >= coreSize) ) return NULL;
        Core* coreptr = holeptr->getCore(jcoreIdx);
        int sectionSize = coreptr->getNumOfSections();

        if( (jsectionIdx < 0) || (jsectionIdx >= sectionSize) ) return NULL;
        Section* sectionptr = coreptr->getSection((int) (jsectionIdx+1)); //FIXME

        if(sectionptr != NULL)
        {
            float minMax[2] = {sectionptr->getMinDepth(),
                               sectionptr->getMaxDepth()};

            jfloatArray minMaxDepths = jenv->NewFloatArray(2);
            jenv->SetFloatArrayRegion(minMaxDepths, 0, 2, minMax);
    
            return minMaxDepths;
        }
        else
        {
            cout << "---> [OOPS!] NULL sectionptr?!" << endl;
            return NULL;
        }
    }
    else
    {
        return NULL;
    }
}

#ifdef __cplusplus
}
#endif

//************************ END JNI FUNCTIONS ********************************//
