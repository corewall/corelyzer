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

// Scenespace coordinates of the single line segment to be drawn for
// a single-section CoreSectionTie, or the three line segments to be
// drawn for a two-section CoreSectionTie. Three segments are needed to draw
// tie lines on top of the two tied sections, and beneath unrelated core
// sections to avoid visual clutter.
struct CoreSectionTieDrawData {
    float ax, ay, bx, by; // CoreSectionTie points A and B

    // Unused for single-section tie.
    // For a two-section tie, (aix,aiy) is intersection of tie line and edge of section A,
    // (bix,biy) is intersection of tie line and edge of section B.
    float aix, aiy, bix, biy;

    void setPointA(const float ax, const float ay) {
        this->ax = ax;
        this->ay = ay;
    }

    void getPointA(float &ax, float &ay) {
        ax = this->ax;
        ay = this->ay;
    }

    void setPointB(const float bx, const float by) {
        this->bx = bx;
        this->by = by;
    }

    void getPointB(float &bx, float &by) {
        bx = this->bx;
        by = this->by;
    }

    void setPointAEdge(const float aix, const float aiy) {
        this->aix = aix;
        this->aiy = aiy;
    }

    void getPointAEdge(float &aix, float &aiy) {
        aix = this->aix;
        aiy = this->aiy;
    }

    void setPointBEdge(const float bix, const float biy) {
        this->bix = bix;
        this->biy = biy;
    }

    void getPointBEdge(float &bix, float &biy) {
        bix = this->bix;
        biy = this->biy;
    }
};

// A tie between two features of interest, points A and B, on section core images.
// Points A and B may be on the same core image.
struct CoreSectionTie {
    SectionTieType type;
    SectionTiePoint *a, *b;

    char *aDesc; // description of point A (Z in UI)
    char *bDesc; // description of point B (Z' in UI)

    bool show; // draw tie line?

    CoreSectionTieDrawData *drawData;

    CoreSectionTie(SectionTieType type, SectionTiePoint &a, SectionTiePoint &b) {
        this->type = type;
        this->a = new SectionTiePoint(a);
        this->b = new SectionTiePoint(b);
        aDesc = NULL;
        bDesc = NULL;
        show = true;
        drawData = new CoreSectionTieDrawData();
    }

    ~CoreSectionTie() {
        if (a) delete a;
        if (b) delete b;
        if (aDesc) delete[] aDesc;
        if (bDesc) delete[] bDesc;
        if (drawData) delete drawData;
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
bool start_section_tie(int tieId, bool fixedPointIsA);
CoreSectionTie *finish_section_tie(int trackId, int sectionId, float x, float y);
void clear_in_progress_tie();

#endif // #ifndef CORELYZER_TIE_H