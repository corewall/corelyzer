#ifndef CORELYZER_TIE_H
#define CORELYZER_TIE_H

#include <stdlib.h>

// A tie between two core section images
struct CoreSectionTie {
    int type; // splice, visual, etc
    int srcTrack;
    int srcCore;
    float x; // x position of tie point...source core?
    float y; // y position of tie point
    int destTrack;
    int destCore;
    float ax; // x,y of destination core?
    float ay;
    char *srcDesc; // description of source tie point
    char *destDesc; // description of destination tie point
    bool complete; // true if both endpoints been defined
    bool selected; // for highlighting tie on selection
    bool show; // if false, don't draw tie line

    CoreSectionTie(int _type, int trackId, int coreId, float _x, float _y) {
        type = _type;
        srcTrack = trackId;
        srcCore = coreId;
        x = _x;
        y = _y;
        destTrack = -1;
        destCore = -1;
        srcDesc = NULL;
        destDesc = NULL;
        complete = false;
        selected = false;
        show = true;
    }

    ~CoreSectionTie() {
        if (srcDesc) delete[] srcDesc;
        if (destDesc) delete[] destDesc;
    }

    void setDestination(int trackId, int coreId, float x, float y);

    void setSourceDescription(char *desc);
    void setDestinationDescription(char *dest);
    char *getSourceDescription();
    char *getDestinationDescription();
};

CoreSectionTie *get_active_tie();
void set_active_tie(CoreSectionTie *tie);
CoreSectionTie* create_section_tie(int type, int trackId, int sectionId, float x, float y);
bool finish_section_tie(CoreSectionTie *tie, int trackId, int sectionId, float x, float y);

#endif // #ifndef CORELYZER_TIE_H