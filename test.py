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
    rsq, sr2, sr6, force = potential(12.895151,2.330429,25.339006,0.36249304,0.7965223,3.9454947,2.353801,24.957212)
    rsq, sr2, sr6, force = potential(6.845665,4.7752852,6.6563506,64.37003,16.460386,41.39874,42.362267,1.8271264)
    rsq, sr2, sr6, force = potential(4.2487392,12.244539,39.71901,4.376286,75.83407,25.81096,1.0490967,89.146)
    print("rsq: ", rsq)
    print("sr2: ", sr2)
    print("sr6: ", sr6)
    print("force: ", force)
