#ifndef CORELYZER_TIE_H
#define CORELYZER_TIE_H

#include <stdlib.h>

// Section-space point on a core section image.
struct SectionTiePoint {
    int track, section;
    float x, y;
    
    SectionTiePoint() {
        track = -1;
        section = -1;
        x = 0.0f;
        y = 0.0f;
    }

    SectionTiePoint(int track, int section, float x, float y) {
        this->track = track;
        this->section = section;
        this->x = x;
        this->y = y;
    }

    SectionTiePoint(SectionTiePoint &tp) {
        track = tp.track;
        section = tp.section;
        x = tp.x;
        y = tp.y;
    }

    void toSceneSpace(float &scenex, float &sceney);
};


// A tie between two features of interest, points A and B, on section core images.
// Points A and B may be on the same core image.
struct CoreSectionTie {
    SectionTiePoint *a, *b;

    char *aDesc; // description of point A
    char *bDesc; // description of point B

    bool selected; // highlight tie on selection
    bool show; // draw tie line?

    CoreSectionTie(SectionTiePoint &a, SectionTiePoint &b) {
        this->a = new SectionTiePoint(a);
        this->b = new SectionTiePoint(b);
        aDesc = NULL;
        bDesc = NULL;
        selected = false;
        show = true;        
    }

    ~CoreSectionTie() {
        if (a) delete a;
        if (b) delete b;
        if (aDesc) delete[] aDesc;
        if (bDesc) delete[] bDesc;
    }

    // getter/setter
    void setADescription(char *desc);
    void setBDescription(char *dest);
    char *getADescription();
    char *getBDescription();
    bool getShow();
    void setShow(bool _show);
};

SectionTiePoint *get_in_progress_tie();
bool start_section_tie(int trackId, int sectionId, float x, float y);
CoreSectionTie *finish_section_tie(int trackid, int sectionId, float x, float y);

#endif // #ifndef CORELYZER_TIE_H