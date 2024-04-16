function getQuestionParticle(c: string, softOffset: number): string {
    return chooseMBP(c, softOffset, MAME, BABE, PAPE);
}

function getColloquialQuestionParticle(c: string, softOffset: number): string {
    return chooseMBP(c, softOffset, CLQ_MAME, CLQ_BABE, CLQ_PAPE);
}