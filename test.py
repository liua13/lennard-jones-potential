def potential(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon):
    delx = m2x - m1x
    dely = m2y - m1y
    delz = m2z - m1z
    rsq = delx * delx + dely * dely + delz * delz
    sr2 = 1.0 / rsq
    sr6 = sr2 * sr2 * sr2 * sigma6
    force = 48.0 * sr6 * (sr6 - 0.5) * sr2 * epsilon
    return rsq, sr2, sr6, force

if __name__ == "__main__":
    rsq, sr2, sr6, force = potential(7.5895433,12.694938,3.5018194,3.935307,22.743324,1.8460276,51.527622,44.758675)
    print("rsq: ", rsq)
    print("sr2: ", sr2)
    print("sr6: ", sr6)
    print("force: ", force)
