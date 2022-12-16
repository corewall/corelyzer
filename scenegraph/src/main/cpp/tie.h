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

    CoreSectionTie(SectionTieType type, SectionTiePoint &a, SectionTiePoint &b) {
        this->type = type;
        this->a = new SectionTiePoint(a);
        this->b = new SectionTiePoint(b);
        aDesc = NULL;
        bDesc = NULL;
        show = true;        
    }

    ~CoreSectionTie() {
        if (a) delete a;
        if (b) delete b;
        if (aDesc) delete[] aDesc;
        if (bDesc) delete[] bDesc;
    }

    // getter/setter
    SectionTieType getType() { return type; }
    void setType(SectionTieType type) { this->type = type; }
    void setADescription(char *desc);
    void setBDescription(char *dest);
    char *getADescription();
    char *getBDescription();
    bool getShow();
    void setShow(bool _show);

    bool valid() { return a->valid() && b->valid(); }
    bool isOnTrack(const int trackId) {
        return a->track == trackId || b->track == trackId;
    }
    bool isOnSection(const int secId) {
        return a->section == secId || b->section == secId;
    }
};

SectionTiePoint *get_in_progress_tie();
SectionTieType get_in_progress_tie_type();
bool start_section_tie(SectionTieType type, int trackId, int sectionId, float x, float y);
CoreSectionTie *finish_section_tie(int trackid, int sectionId, float x, float y);
void clear_in_progress_tie();

#endif // #ifndef CORELYZER_TIE_H