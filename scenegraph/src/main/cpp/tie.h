#ifndef CORELYZER_TIE_H
#define CORELYZER_TIE_H

// A tie between two core section images
struct CoreSectionTie {
    int type; // splice, visual, etc
    float x; // x position of tie point...source core?
    float y; // y position of tie point
    int destCore;
    int destTrack;
    float ax; // x,y of destination core?
    float ay;
    char *sourceDesc; // description of source tie point
    char *destDesc; // description of destination tie point
    bool complete;

    CoreSectionTie(int _type, float _x, float _y) {
        type = _type;
        x = _x;
        y = _y;
        destCore = -1;
        destTrack = -1;
        complete = false;
    }

    ~CoreSectionTie() {
        // todo: free description memory
    }

    void setDestination(int coreId, int trackId, float x, float y) {
        destCore = coreId;
        destTrack = trackId;
        ax = x;
        ay = y;
        complete = true;
    }
};

void render_tie(CoreSectionTie *t);



#endif // #ifndef CORELYZER_TIE_H