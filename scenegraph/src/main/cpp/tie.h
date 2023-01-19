#ifndef CORELYZER_TIE_H
#define CORELYZER_TIE_H

#include <stdlib.h>

enum SectionTieType {
    NONE = 0,
    VISUAL,
    DATA,
    SPLICE
};

const int TIE_SELECT_DIST_PIX = 5;

// Section-space point on a core section image.
struct SectionTiePoint {
    int trackId, sectionId;
    float x, y;
    
    SectionTiePoint() {
        trackId = -1;
        sectionId = -1;
        x = 0.0f;
        y = 0.0f;
    }

    SectionTiePoint(int trackId, int sectionId, float x, float y) {
        this->trackId = trackId;
        this->sectionId = sectionId;
        this->x = x;
        this->y = y;
    }

    SectionTiePoint(SectionTiePoint &tp) {
        trackId = tp.trackId;
        sectionId = tp.sectionId;
        x = tp.x;
        y = tp.y;
    }

    void toSceneSpace(float &scenex, float &sceney);
    bool valid();
};


// A tie between two features of interest, points A and B, on section core images.
// Points A and B may be on the same core image.
struct CoreSectionTie {
    SectionTieType type;
    SectionTiePoint *a, *b;

    char *aDesc; // description of point A (Z in UI)
    char *bDesc; // description of point B (Z' in UI)

    bool show; // draw tie line?

    float *segments; // array of (x,y) scenespace coordinates for segments of tie line

    CoreSectionTie(SectionTieType type, SectionTiePoint &a, SectionTiePoint &b) {
        this->type = type;
        this->a = new SectionTiePoint(a);
        this->b = new SectionTiePoint(b);
        aDesc = NULL;
        bDesc = NULL;
        show = true;
        segments = NULL;        
    }

    ~CoreSectionTie() {
        if (a) delete a;
        if (b) delete b;
        if (aDesc) delete[] aDesc;
        if (bDesc) delete[] bDesc;
        if (segments) delete[] segments;
    }

    // getter/setter
    SectionTieType getType() { return type; }
    void setType(SectionTieType type) { this->type = type; }
    void setADescription(char *desc);
    void setBDescription(char *desc);
    char *getADescription();
    char *getBDescription();
    bool getShow();
    void setShow(bool _show);

    bool valid() { return a->valid() && b->valid(); }
    bool isOnTrack(const int trackId) {
        return a->trackId == trackId || b->trackId == trackId;
    }
    bool isOnSection(const int secId) {
        return a->sectionId == secId || b->sectionId == secId;
    }
    bool isSingleSection() { // are both tie points on the same section?
        return a->trackId == b->trackId && a->sectionId == b->sectionId;
    }
};

SectionTiePoint *get_in_progress_tie();
SectionTieType get_in_progress_tie_type();
bool start_section_tie(SectionTieType type, int trackId, int sectionId, float x, float y);
CoreSectionTie *finish_section_tie(int trackId, int sectionId, float x, float y);
void clear_in_progress_tie();

#endif // #ifndef CORELYZER_TIE_H