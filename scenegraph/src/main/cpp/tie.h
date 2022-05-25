#ifndef CORELYZER_TIE_H
#define CORELYZER_TIE_H

#include <stdlib.h>

// A tie between two features of interest, points A and B, on 1 or 2 core
// section images.
struct CoreSectionTie {
    int type; // splice, visual, etc
    int aTrack; // Point A
    int aCore;
    float ax; // core-relative
    float ay;

    int bTrack; // Point B
    int bCore;
    float bx; // core-relative
    float by;

    char *aDesc; // description of point A
    char *bDesc; // description of point B

    bool complete; // true if both endpoints been defined
    bool selected; // for highlighting tie on selection
    bool show; // if false, don't draw tie line

    CoreSectionTie(int _type, int trackId, int coreId, float _x, float _y) {
        type = _type;
        aTrack = trackId;
        aCore = coreId;
        ax = _x;
        ay = _y;
        bTrack = -1;
        bCore = -1;
        aDesc = NULL;
        bDesc = NULL;
        complete = false;
        selected = false;
        show = true;
    }

    ~CoreSectionTie() {
        if (aDesc) delete[] aDesc;
        if (bDesc) delete[] bDesc;
    }

    void setB(int trackId, int coreId, float x, float y);

    // getter/setter
    void setADescription(char *desc);
    void setBDescription(char *dest);
    char *getADescription();
    char *getBDescription();
    bool getShow();
    void setShow(bool _show);
};

CoreSectionTie *get_active_tie();
void set_active_tie(CoreSectionTie *tie);
CoreSectionTie* create_section_tie(int type, int trackId, int sectionId, float x, float y);
bool finish_section_tie(CoreSectionTie *tie, int trackId, int sectionId, float x, float y);

#endif // #ifndef CORELYZER_TIE_H